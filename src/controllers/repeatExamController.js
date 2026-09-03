const mongoose = require('mongoose');
const RepeatExam = require('../models/RepeatExam');
const ExamResult = require('../models/ExamResult');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { gradeFor } = require('../services/gradeScale');
const { recalculateSemesterGpa } = require('../services/recalculateSemesterGpa');
const { RepeatStatus, UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');
const { notifyUsers, notifyAfterResponse, resolveHodOfDepartment, resolveAllAdmins } = require('../services/notificationService');

const POPULATE_SUBJECT = { path: 'subjectId', select: 'code title creditHours departmentId semesterId' };
const POPULATE_STUDENT = { path: 'studentId', select: 'fullName rollNumber registrationNumber' };
const POPULATE_ALL = [POPULATE_SUBJECT, POPULATE_STUDENT];

async function requireReviewer(req, departmentId) {
  if (req.user.role !== UserRole.ADMIN && !isHodOfDepartment(req.user, departmentId)) {
    throw new ApiError(403, 'Only the HOD of this department or an Admin can review repeat exams');
  }
}

/** Schedules a repeat for a failed, repeat-eligible ExamResult. Callable by the subject's
 * teacher, its department's HOD, or an Admin - matches the app's existing pattern of
 * letting more than one role kick off a workflow that only HOD/Admin can later approve. */
const create = asyncHandler(async (req, res) => {
  const { examResultId } = req.body;
  if (!examResultId) throw new ApiError(400, 'examResultId is required');

  const examResult = await ExamResult.findById(examResultId);
  if (!examResult) throw new ApiError(404, 'Exam result not found');
  if (!examResult.repeatEligible || examResult.status !== 'FAIL') {
    throw new ApiError(400, 'This result is not eligible for a repeat exam');
  }

  const subject = await Subject.findById(examResult.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher && !isHodOfDepartment(req.user, examResult.departmentId)) {
    throw new ApiError(403, 'Only the subject\'s teacher, its HOD, or an Admin can schedule a repeat exam');
  }

  const existingActive = await RepeatExam.findOne({
    examResultId,
    repeatStatus: { $in: [RepeatStatus.PENDING, RepeatStatus.SUBMITTED] },
  });
  if (existingActive) {
    throw new ApiError(409, 'A repeat exam is already scheduled or pending review for this result');
  }

  const repeatExam = await RepeatExam.create({
    examResultId,
    studentId: examResult.studentId,
    subjectId: examResult.subjectId,
    departmentId: examResult.departmentId,
    previousMarks: examResult.marks,
    previousGrade: examResult.grade,
    previousGpa: examResult.gpa,
    repeatStatus: RepeatStatus.PENDING,
  });
  await repeatExam.populate(POPULATE_ALL);

  res.status(201).json({ repeatExam });

  notifyAfterResponse(async () => {
    await notifyUsers([repeatExam.studentId._id.toString()], {
      type: NotificationType.REPEAT_EXAM_SCHEDULED,
      title: `Repeat exam scheduled: ${repeatExam.subjectId && repeatExam.subjectId.title ? repeatExam.subjectId.title : ''}`,
      body: `Previous marks: ${repeatExam.previousMarks} (${repeatExam.previousGrade})`,
      actorId: req.user._id.toString(),
      relatedType: 'repeatExam',
      relatedId: repeatExam._id.toString(),
    });
  });
});

/** Teacher enters the repeat's marks - same submit step as a normal result, scoped to
 * the one subject/student pair being repeated. */
const submitMarks = asyncHandler(async (req, res) => {
  const { marks } = req.body;
  if (typeof marks !== 'number' || marks < 0 || marks > 100) {
    throw new ApiError(400, 'marks must be a number between 0 and 100');
  }

  const repeatExam = await RepeatExam.findById(req.params.id);
  if (!repeatExam) throw new ApiError(404, 'Repeat exam not found');
  if (![RepeatStatus.PENDING, RepeatStatus.REJECTED].includes(repeatExam.repeatStatus)) {
    throw new ApiError(400, `Cannot submit marks while status is ${repeatExam.repeatStatus}`);
  }

  const subject = await Subject.findById(repeatExam.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can submit repeat exam marks');
  }

  const { grade, gpa } = gradeFor(marks);
  repeatExam.newMarks = marks;
  repeatExam.newGrade = grade;
  repeatExam.newGpa = gpa;
  repeatExam.repeatStatus = RepeatStatus.SUBMITTED;
  repeatExam.rejectionReason = null;
  repeatExam.submittedBy = req.user._id.toString();
  await repeatExam.save();
  await repeatExam.populate(POPULATE_ALL);

  res.json({ repeatExam });

  notifyAfterResponse(async () => {
    const [hod, admins] = await Promise.all([
      resolveHodOfDepartment(repeatExam.departmentId),
      resolveAllAdmins(),
    ]);
    await notifyUsers([...hod, ...admins], {
      type: NotificationType.REPEAT_EXAM_MARKS_SUBMITTED,
      title: `Repeat exam marks submitted: ${repeatExam.subjectId && repeatExam.subjectId.title ? repeatExam.subjectId.title : ''}`,
      body: `New marks: ${repeatExam.newMarks} (${repeatExam.newGrade})`,
      actorId: req.user._id.toString(),
      relatedType: 'repeatExam',
      relatedId: repeatExam._id.toString(),
    });
  });
});

/** On approval, the ORIGINAL ExamResult is updated in place with the repeat's marks
 * (previousMarks/Grade/Gpa on the RepeatExam doc keep the history) and semester GPA/CGPA
 * is recalculated - same side effects as approving a normal result (spec §8). */
const approve = asyncHandler(async (req, res) => {
  const repeatExam = await RepeatExam.findById(req.params.id);
  if (!repeatExam) throw new ApiError(404, 'Repeat exam not found');
  if (repeatExam.repeatStatus !== RepeatStatus.SUBMITTED) {
    throw new ApiError(400, `Cannot approve a repeat exam in status ${repeatExam.repeatStatus}`);
  }

  const examResult = await ExamResult.findById(repeatExam.examResultId);
  if (!examResult) throw new ApiError(404, 'Original exam result not found');
  await requireReviewer(req, examResult.departmentId);

  const previousValues = { marks: examResult.marks, grade: examResult.grade, gpa: examResult.gpa, status: examResult.status };
  examResult.marks = repeatExam.newMarks;
  examResult.grade = repeatExam.newGrade;
  examResult.gpa = repeatExam.newGpa;
  examResult.status = repeatExam.newGrade === 'F' ? 'FAIL' : 'PASS';
  examResult.repeatEligible = examResult.status === 'FAIL';
  examResult.auditLog.push({
    action: 'REPEAT_EXAM_APPROVED',
    by: req.user._id.toString(),
    at: new Date(),
    previousValues,
    newValues: { marks: examResult.marks, grade: examResult.grade, gpa: examResult.gpa, status: examResult.status },
  });

  repeatExam.repeatStatus = RepeatStatus.APPROVED;
  repeatExam.reviewedBy = req.user._id.toString();
  repeatExam.reviewedAt = new Date();

  // ExamResult + RepeatExam + StudentSemesterResult all move together - same
  // transaction requirement as a normal result approval.
  const session = await mongoose.startSession();
  try {
    await session.withTransaction(async () => {
      await examResult.save({ session });
      await repeatExam.save({ session });
      await recalculateSemesterGpa(examResult.studentId, examResult.semesterId, { session });
    });
  } finally {
    await session.endSession();
  }

  await repeatExam.populate(POPULATE_ALL);

  res.json({ repeatExam, examResult });

  notifyAfterResponse(async () => {
    await notifyUsers([repeatExam.studentId._id.toString()], {
      type: NotificationType.REPEAT_EXAM_APPROVED,
      title: `Repeat exam approved: ${repeatExam.subjectId && repeatExam.subjectId.title ? repeatExam.subjectId.title : ''}`,
      body: `New grade: ${repeatExam.newGrade} (GPA ${repeatExam.newGpa})`,
      actorId: req.user._id.toString(),
      relatedType: 'repeatExam',
      relatedId: repeatExam._id.toString(),
    });
  });
});

const reject = asyncHandler(async (req, res) => {
  const { reason } = req.body;
  if (!reason || !reason.trim()) {
    throw new ApiError(400, 'reason is required to reject a repeat exam');
  }

  const repeatExam = await RepeatExam.findById(req.params.id);
  if (!repeatExam) throw new ApiError(404, 'Repeat exam not found');
  if (repeatExam.repeatStatus !== RepeatStatus.SUBMITTED) {
    throw new ApiError(400, `Cannot reject a repeat exam in status ${repeatExam.repeatStatus}`);
  }

  const examResult = await ExamResult.findById(repeatExam.examResultId);
  await requireReviewer(req, examResult.departmentId);

  repeatExam.repeatStatus = RepeatStatus.REJECTED;
  repeatExam.rejectionReason = reason.trim();
  repeatExam.reviewedBy = req.user._id.toString();
  repeatExam.reviewedAt = new Date();
  await repeatExam.save();
  await repeatExam.populate(POPULATE_ALL);

  res.json({ repeatExam });

  if (repeatExam.submittedBy) {
    notifyAfterResponse(async () => {
      await notifyUsers([repeatExam.submittedBy], {
        type: NotificationType.REPEAT_EXAM_REJECTED,
        title: `Repeat exam rejected: ${repeatExam.subjectId && repeatExam.subjectId.title ? repeatExam.subjectId.title : ''}`,
        body: repeatExam.rejectionReason,
        actorId: req.user._id.toString(),
        relatedType: 'repeatExam',
        relatedId: repeatExam._id.toString(),
      });
    });
  }
});

const listForStudent = asyncHandler(async (req, res) => {
  const studentId = req.params.studentId === 'me' ? req.user._id.toString() : req.params.studentId;
  const repeatExams = await RepeatExam.find({ studentId }).sort({ createdAt: -1 }).populate(POPULATE_ALL);
  res.json({ repeatExams });
});

/** Teacher must scope by subjectId (and own it); HOD is auto-scoped to their own
 * department (optionally further filtered to one subjectId); Admin sees everything. */
const list = asyncHandler(async (req, res) => {
  const { subjectId, status } = req.query;
  const filter = {};
  if (status) filter.repeatStatus = status;

  if (req.user.role === UserRole.TEACHER) {
    if (!subjectId) throw new ApiError(400, 'subjectId is required');
    const subject = await Subject.findById(subjectId);
    if (!subject) throw new ApiError(404, 'Subject not found');
    if (subject.teacherId !== req.user._id.toString()) {
      throw new ApiError(403, 'Only this subject\'s assigned teacher can view its repeat exams');
    }
    filter.subjectId = subjectId;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
    if (subjectId) filter.subjectId = subjectId;
  } else {
    if (subjectId) filter.subjectId = subjectId;
  }

  const repeatExams = await RepeatExam.find(filter).sort({ createdAt: -1 }).populate(POPULATE_ALL);
  res.json({ repeatExams });
});

module.exports = { create, submitMarks, approve, reject, listForStudent, list };
