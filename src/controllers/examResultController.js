const mongoose = require('mongoose');
const ExamResult = require('../models/ExamResult');
const Subject = require('../models/Subject');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { gradeFor } = require('../services/gradeScale');
const { recalculateSemesterGpa } = require('../services/recalculateSemesterGpa');
const { ResultStatus, UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');
const { notifyUsers, notifyAfterResponse, resolveHodOfDepartment, resolveAllAdmins } = require('../services/notificationService');

const EDITABLE_STATUSES = [ResultStatus.DRAFT, ResultStatus.REJECTED];

// Every ExamResult response is populated with just enough subject/student display fields
// that list/queue screens (Teacher's entry list, HOD's approval queue, Admin's monitor)
// don't need N+1 lookups just to show a name/title next to each row.
const POPULATE_SUBJECT = { path: 'subjectId', select: 'code title creditHours departmentId semesterId' };
const POPULATE_STUDENT = { path: 'studentId', select: 'fullName rollNumber registrationNumber' };
const POPULATE_TEACHER = { path: 'submittedBy', select: 'fullName' };
const POPULATE_ALL = [POPULATE_SUBJECT, POPULATE_STUDENT, POPULATE_TEACHER];

async function requireOwnSubject(req, subjectId) {
  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');
  const isAssignedTeacher = subject.teacherId && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can submit marks for it');
  }
  return subject;
}

function validateResultsPayload(results) {
  if (!Array.isArray(results) || results.length === 0) {
    throw new ApiError(400, 'results must be a non-empty array of { studentId, marks }');
  }
  for (const r of results) {
    if (!r.studentId) throw new ApiError(400, 'Each result requires a studentId');
    if (typeof r.marks !== 'number' || r.marks < 0 || r.marks > 100) {
      throw new ApiError(400, `marks for student ${r.studentId} must be a number between 0 and 100`);
    }
  }
}

async function upsertResults(req, res, targetStatus) {
  const { subjectId, results } = req.body;
  if (!subjectId) throw new ApiError(400, 'subjectId is required');
  validateResultsPayload(results);

  const subject = await requireOwnSubject(req, subjectId);

  const studentIds = results.map((r) => r.studentId);
  const existing = await ExamResult.find({ subjectId, studentId: { $in: studentIds } });
  const existingByStudent = new Map(existing.map((e) => [e.studentId, e]));

  const blocked = existing.filter((e) => !EDITABLE_STATUSES.includes(e.resultStatus));
  if (blocked.length > 0) {
    throw new ApiError(400, `These students' results are no longer editable (already ${blocked[0].resultStatus}): ${blocked.map((b) => b.studentId).join(', ')}`);
  }

  const saved = [];
  for (const r of results) {
    const { grade, gpa, status } = gradeFor(r.marks);
    const existingDoc = existingByStudent.get(r.studentId);

    const fields = {
      studentId: r.studentId,
      subjectId,
      departmentId: subject.departmentId,
      semesterId: subject.semesterId,
      marks: r.marks,
      grade,
      gpa,
      status,
      resultStatus: targetStatus,
      rejectionReason: null,
      repeatEligible: status === 'FAIL',
      submittedBy: req.user._id.toString(),
    };

    const doc = await ExamResult.findOneAndUpdate(
      { studentId: r.studentId, subjectId },
      fields,
      { new: true, upsert: true, setDefaultsOnInsert: true }
    );
    await doc.populate(POPULATE_ALL);
    saved.push(doc);

    if (targetStatus === ResultStatus.APPROVED) {
      await recalculateSemesterGpa(r.studentId, subject.semesterId);
    }
  }

  res.status(existingByStudent.size > 0 ? 200 : 201).json({ results: saved });

  if (targetStatus === ResultStatus.PENDING_HOD_APPROVAL) {
    notifyAfterResponse(async () => {
      const [hod, admins] = await Promise.all([
        resolveHodOfDepartment(subject.departmentId),
        resolveAllAdmins(),
      ]);
      await notifyUsers([...hod, ...admins], {
        type: NotificationType.EXAM_RESULT_SUBMITTED_FOR_APPROVAL,
        title: `Results submitted for approval: ${subject.title || subject.code || ''}`,
        body: `${results.length} student(s)`,
        actorId: req.user._id.toString(),
        relatedType: 'subject',
        relatedId: subject._id.toString(),
      });
    });
  }
}

/** Teachers have no path to mark a result APPROVED directly - draft/submit only ever
 * produce DRAFT or PENDING_HOD_APPROVAL, enforced by targetStatus being fixed here, not
 * client-supplied. */
const saveDraft = asyncHandler((req, res) => upsertResults(req, res, ResultStatus.DRAFT));
const submit = asyncHandler((req, res) => upsertResults(req, res, ResultStatus.PENDING_HOD_APPROVAL));

const approve = asyncHandler(async (req, res) => {
  const result = await ExamResult.findById(req.params.id);
  if (!result) throw new ApiError(404, 'Result not found');
  if (req.user.role !== UserRole.ADMIN && !isHodOfDepartment(req.user, result.departmentId)) {
    throw new ApiError(403, 'Only the HOD of this department or an Admin can approve results');
  }
  if (result.resultStatus !== ResultStatus.PENDING_HOD_APPROVAL) {
    throw new ApiError(400, `Cannot approve a result in status ${result.resultStatus}`);
  }

  const previousValues = { resultStatus: result.resultStatus, marks: result.marks, grade: result.grade, gpa: result.gpa };
  result.resultStatus = ResultStatus.APPROVED;
  result.reviewedBy = req.user._id.toString();
  result.reviewedAt = new Date();
  result.auditLog.push({
    action: 'APPROVE',
    by: req.user._id.toString(),
    at: new Date(),
    previousValues,
    newValues: { resultStatus: result.resultStatus },
  });

  // Result save + GPA/CGPA rollup touch separate documents (ExamResult, StudentSemesterResult)
  // - a single transaction keeps them from diverging if the process dies mid-sequence (spec's
  // cross-cutting "idempotency & transactions" requirement).
  const session = await mongoose.startSession();
  try {
    await session.withTransaction(async () => {
      await result.save({ session });
      await recalculateSemesterGpa(result.studentId, result.semesterId, { session });
    });
  } finally {
    await session.endSession();
  }

  await result.populate(POPULATE_ALL);

  res.json({ result });

  notifyAfterResponse(async () => {
    await notifyUsers([result.studentId._id.toString()], {
      type: NotificationType.EXAM_RESULT_APPROVED,
      title: `Result approved: ${result.subjectId && result.subjectId.title ? result.subjectId.title : ''}`,
      body: `Grade: ${result.grade} (GPA ${result.gpa})`,
      actorId: req.user._id.toString(),
      relatedType: 'examResult',
      relatedId: result._id.toString(),
    });
  });
});

const reject = asyncHandler(async (req, res) => {
  const { reason } = req.body;
  if (!reason || !reason.trim()) {
    throw new ApiError(400, 'reason is required to reject a result');
  }

  const result = await ExamResult.findById(req.params.id);
  if (!result) throw new ApiError(404, 'Result not found');
  if (req.user.role !== UserRole.ADMIN && !isHodOfDepartment(req.user, result.departmentId)) {
    throw new ApiError(403, 'Only the HOD of this department or an Admin can reject results');
  }
  if (result.resultStatus !== ResultStatus.PENDING_HOD_APPROVAL) {
    throw new ApiError(400, `Cannot reject a result in status ${result.resultStatus}`);
  }

  const previousValues = { resultStatus: result.resultStatus };
  result.resultStatus = ResultStatus.REJECTED;
  result.rejectionReason = reason.trim();
  result.reviewedBy = req.user._id.toString();
  result.reviewedAt = new Date();
  result.auditLog.push({
    action: 'REJECT',
    by: req.user._id.toString(),
    at: new Date(),
    previousValues,
    newValues: { resultStatus: result.resultStatus, rejectionReason: result.rejectionReason },
  });
  await result.save();
  await result.populate(POPULATE_ALL);

  res.json({ result });

  notifyAfterResponse(async () => {
    await notifyUsers([result.submittedBy._id ? result.submittedBy._id.toString() : result.submittedBy.toString()], {
      type: NotificationType.EXAM_RESULT_REJECTED,
      title: `Result rejected: ${result.subjectId && result.subjectId.title ? result.subjectId.title : ''}`,
      body: result.rejectionReason,
      actorId: req.user._id.toString(),
      relatedType: 'examResult',
      relatedId: result._id.toString(),
    });
  });
});

/** Role-scoped list/monitor endpoint - Admin sees everything (with optional filters,
 * matching the spec's admin-monitoring query); HOD is forced to their own department;
 * Teacher is forced to results they submitted; Student is forced to their own APPROVED
 * results only. */
const list = asyncHandler(async (req, res) => {
  const { status, departmentId, semesterId, subjectId, studentId, repeatEligible } = req.query;
  const filter = {};
  if (repeatEligible !== undefined) filter.repeatEligible = repeatEligible === 'true';

  if (req.user.role === UserRole.ADMIN) {
    if (status) filter.resultStatus = status;
    if (departmentId) filter.departmentId = departmentId;
    if (studentId) filter.studentId = studentId;
    if (req.query.submittedBy) filter.submittedBy = req.query.submittedBy;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
    if (status) filter.resultStatus = status;
    if (studentId) filter.studentId = studentId;
  } else if (req.user.role === UserRole.TEACHER) {
    filter.submittedBy = req.user._id.toString();
    if (status) filter.resultStatus = status;
  } else {
    filter.studentId = req.user._id.toString();
    filter.resultStatus = ResultStatus.APPROVED;
  }

  if (semesterId) filter.semesterId = semesterId;
  if (subjectId) filter.subjectId = subjectId;

  const results = await ExamResult.find(filter).sort({ updatedAt: -1 }).populate(POPULATE_ALL);
  res.json({ results });
});

const POPULATE_SEMESTER_RESULT = [
  { path: 'semesterId', select: 'number departmentId' },
  { path: 'sessionId', select: 'label' },
  { path: 'studentId', select: 'fullName rollNumber registrationNumber' },
];

/** Read-optimized Results section (separate from the monitor/approval/entry screens above) -
 * exposes StudentSemesterResult's already-computed per-semester rollup (subjectResults, GPA,
 * CGPA, resultStatus, promotionStatus) with subject-level detail, which the summary-only
 * `dashboard` endpoint above deliberately doesn't return. Nothing is recomputed here; this
 * only reads what recalculateSemesterGpa already wrote. Role-scoped: Student is forced to
 * their own record; HOD is forced to their own department; Teacher has no browsing use case
 * here (they use the existing subject-scoped /results list instead) and is rejected; Admin is
 * unrestricted, with optional departmentId/sessionId/semesterId/studentId filters. */
const listSemesterResults = asyncHandler(async (req, res) => {
  if (req.user.role === UserRole.TEACHER) {
    throw new ApiError(403, 'Teachers view results per-subject via the results list endpoint');
  }

  const { departmentId, sessionId, semesterId, studentId } = req.query;
  const filter = {};

  if (req.user.role === UserRole.STUDENT) {
    // APPROVED is the only status a Student is ever allowed to see (same rule the flat
    // /results list enforces for them) - DRAFT/PENDING_HOD_APPROVAL/REJECTED semester
    // rollups stay internal to Teacher/HOD/Admin until published.
    filter.studentId = req.user._id.toString();
    filter.resultStatus = ResultStatus.APPROVED;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
    if (studentId) filter.studentId = studentId;
  } else {
    if (departmentId) filter.departmentId = departmentId;
    if (studentId) filter.studentId = studentId;
  }

  if (sessionId) filter.sessionId = sessionId;
  if (semesterId) filter.semesterId = semesterId;

  const semesterResults = await StudentSemesterResult.find(filter)
    .sort({ updatedAt: -1 })
    .populate(POPULATE_SEMESTER_RESULT);
  res.json({ semesterResults });
});

module.exports = { saveDraft, submit, approve, reject, list, listSemesterResults };
