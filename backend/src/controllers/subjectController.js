const mongoose = require('mongoose');
const Subject = require('../models/Subject');
const User = require('../models/User');
const Assignment = require('../models/Assignment');
const AssignmentSubmission = require('../models/AssignmentSubmission');
const AttendanceRecord = require('../models/AttendanceRecord');
const ExamResult = require('../models/ExamResult');
const ExamSchedule = require('../models/ExamSchedule');
const Quiz = require('../models/Quiz');
const QuizAttempt = require('../models/QuizAttempt');
const RepeatExam = require('../models/RepeatExam');
const StudyMaterial = require('../models/StudyMaterial');
const TimetableSlot = require('../models/TimetableSlot');
const Promotion = require('../models/Promotion');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');

function describeFootprint(footprint) {
  return Object.entries(footprint)
    .filter(([, count]) => count > 0)
    .map(([key, count]) => `${count} ${key.replace(/([A-Z])/g, ' $1').toLowerCase().trim()}`)
    .join(', ');
}

const POPULATE_TEACHER = { path: 'teacherId', select: 'fullName' };

const listForSemester = asyncHandler(async (req, res) => {
  const subjects = await Subject.find({ semesterId: req.params.semesterId }).sort({ code: 1 }).populate(POPULATE_TEACHER);
  res.json({ subjects });
});

const listForDepartment = asyncHandler(async (req, res) => {
  const subjects = await Subject.find({ departmentId: req.params.departmentId }).sort({ code: 1 }).populate(POPULATE_TEACHER);
  res.json({ subjects });
});

/** Cross-department: a Teacher's subjects can span every department they've joined (see
 * User.departmentIds), mirroring the old observeSubjectsForTeacher() cross-department query. */
const listForTeacher = asyncHandler(async (req, res) => {
  const teacherId = req.params.teacherId === 'me' ? req.user._id.toString() : req.params.teacherId;
  const subjects = await Subject.find({ teacherId }).sort({ code: 1 }).populate(POPULATE_TEACHER);
  res.json({ subjects });
});

const create = asyncHandler(async (req, res) => {
  const { departmentId, semesterId, code, title, creditHours } = req.body;
  if (!departmentId || !semesterId || !code || !title || !creditHours) {
    throw new ApiError(400, 'departmentId, semesterId, code, title, and creditHours are required');
  }
  let subject;
  try {
    subject = await Subject.create({ departmentId, semesterId, code, title, creditHours });
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, `A subject with code "${code}" already exists in this department`);
    throw err;
  }
  res.status(201).json({ subject });
});

/** Admin-only, matching the pre-migration app (semesterId/departmentId are deliberately not
 * editable here - moving a subject between semesters isn't a supported flow). */
const update = asyncHandler(async (req, res) => {
  const { code, title, creditHours } = req.body;
  const subject = await Subject.findById(req.params.id);
  if (!subject) throw new ApiError(404, 'Subject not found');

  if (code !== undefined) subject.code = code;
  if (title !== undefined) subject.title = title;
  if (creditHours !== undefined) subject.creditHours = creditHours;

  try {
    await subject.save();
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, `A subject with code "${code}" already exists in this department`);
    throw err;
  }
  await subject.populate(POPULATE_TEACHER);
  res.json({ subject });
});

/** A subject with any graded/attended/submitted history IS that history's anchor - every
 * Results/Attendance/Reports screen in the app queries by subjectId, so deleting it would make
 * that data unreachable through any normal path, indistinguishable from destroying it. Blocked
 * outright (409, named counts) rather than silently orphaned. A subject with zero footprint
 * (freshly created, or every enrolled student later cleared) has nothing to protect - its
 * unused course-setup rows (assignments/quizzes/material/schedule with no submissions/attempts
 * against them) are cascade-deleted alongside it, in one transaction. */
const remove = asyncHandler(async (req, res) => {
  const subject = await Subject.findById(req.params.id);
  if (!subject) throw new ApiError(404, 'Subject not found');

  // StudentSemesterResult is a read-optimized rollup rebuilt entirely from ExamResult by
  // recalculateSemesterGpa.js - it never holds a subject's history independently of an
  // ExamResult, so checking ExamResult directly (below) already covers it without needing a
  // second, easily-mismatched query into its nested subjectResults[] snapshot array.
  const [examResults, repeatExams, attendanceRecords, assignmentSubmissions, quizAttempts] = await Promise.all([
    ExamResult.countDocuments({ subjectId: subject._id }),
    RepeatExam.countDocuments({ subjectId: subject._id }),
    AttendanceRecord.countDocuments({ subjectId: subject._id }),
    AssignmentSubmission.countDocuments({ subjectId: subject._id }),
    QuizAttempt.countDocuments({ subjectId: subject._id }),
  ]);
  const footprint = { examResults, repeatExams, attendanceRecords, assignmentSubmissions, quizAttempts };
  if (Object.values(footprint).some((count) => count > 0)) {
    throw new ApiError(409, `This subject has existing academic records (${describeFootprint(footprint)}) and cannot be permanently deleted.`);
  }

  const session = await mongoose.startSession();
  try {
    await session.withTransaction(async () => {
      await Assignment.deleteMany({ subjectId: subject._id }, { session });
      await Quiz.deleteMany({ subjectId: subject._id }, { session });
      await StudyMaterial.deleteMany({ subjectId: subject._id }, { session });
      await ExamSchedule.deleteMany({ subjectId: subject._id }, { session });
      await TimetableSlot.deleteMany({ subjectId: subject._id }, { session });
      await User.updateMany({ retakeSubjectIds: subject._id }, { $pull: { retakeSubjectIds: subject._id } }, { session });
      await Promotion.updateMany({ failedSubjectIds: subject._id }, { $pull: { failedSubjectIds: subject._id } }, { session });
      await subject.deleteOne({ session });
    });
  } finally {
    await session.endSession();
  }

  res.status(204).send();
});

/** HOD (of the subject's own department) or Admin can assign any teacher directly, or clear
 * the assignment by sending `unassign: true` instead of a teacherId - a boolean flag rather
 * than relying on the client sending a literal JSON null (Gson and similar clients often
 * omit null fields entirely on serialize, which would otherwise silently no-op). */
const assignTeacher = asyncHandler(async (req, res) => {
  const { teacherId, unassign } = req.body;
  if (!teacherId && !unassign) throw new ApiError(400, 'teacherId is required (or unassign: true)');

  const subject = await Subject.findById(req.params.id);
  if (!subject) throw new ApiError(404, 'Subject not found');

  const isHodOfDepartment = req.user.role === UserRole.HOD
    && req.user.departmentId
    && req.user.departmentId.toString() === subject.departmentId.toString();
  if (req.user.role !== UserRole.ADMIN && !isHodOfDepartment) {
    throw new ApiError(403, 'Only the HOD of this department or an Admin can assign a teacher');
  }

  subject.teacherId = unassign ? null : teacherId;
  await subject.save();
  await subject.populate(POPULATE_TEACHER);
  res.json({ subject });
});

/** Teacher self-assignment: no approval needed, but only onto a subject that's (a) still
 * unclaimed and (b) in a department the teacher has already joined (see userController.
 * joinDepartment) - mirrors the pre-migration Firestore rule (subject.teacherUid unclaimed). */
const selfAssign = asyncHandler(async (req, res) => {
  const subject = await Subject.findById(req.params.id);
  if (!subject) throw new ApiError(404, 'Subject not found');

  if (subject.teacherId) {
    throw new ApiError(409, 'This subject is already assigned to a teacher');
  }
  const hasJoinedDepartment = (req.user.departmentIds || []).some(
    (id) => id.toString() === subject.departmentId.toString()
  );
  if (!hasJoinedDepartment) {
    throw new ApiError(403, 'Join this subject\'s department before self-assigning to it');
  }

  subject.teacherId = req.user._id.toString();
  await subject.save();
  await subject.populate(POPULATE_TEACHER);
  res.json({ subject });
});

/** A subject's enrolled roster: every STUDENT currently placed in this subject's
 * (department, semester) pair, plus anyone retaking it from a prior semester (their
 * User.retakeSubjectIds contains this subject) - deduped, matching the old app's merged
 * "normal roster + retake roster" behavior. Scoped to the subject's own teacher, its
 * department's HOD, or an Admin - not exposed as a general-purpose user search. */
const roster = asyncHandler(async (req, res) => {
  const subject = await Subject.findById(req.params.id);
  if (!subject) throw new ApiError(404, 'Subject not found');

  const isAssignedTeacher = subject.teacherId && subject.teacherId === req.user._id.toString();
  const isHod = isHodOfDepartment(req.user, subject.departmentId);
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher && !isHod) {
    throw new ApiError(403, 'Not authorized to view this subject\'s roster');
  }

  const students = await User.find({
    role: UserRole.STUDENT,
    $or: [
      { departmentId: subject.departmentId, currentSemesterId: subject.semesterId },
      { retakeSubjectIds: subject._id },
    ],
  }).sort({ fullName: 1 });

  res.json({ students: students.map((s) => s.toPublicJSON()) });
});

/** The authoritative "my students" list for the currently authenticated Teacher - the union
 * of every subject they're assigned to's roster (same department+semester-pair-or-retake
 * definition as roster() above), deduped by MongoDB's own document identity (a student
 * matching more than one $or branch still comes back as exactly one document) rather than by
 * merging N separate per-subject calls client-side. Teacher identity comes only from
 * req.user - the route requires the TEACHER role and never reads an id from the request. Each
 * student is annotated with which of the teacher's own subjects they actually belong to, so the
 * client can show "Courses: X, Y" per student without a second round-trip; each subject is
 * annotated with its own roster size for the "My Assigned Courses" course-wise counts. */
const myStudents = asyncHandler(async (req, res) => {
  const teacherId = req.user._id.toString();
  const subjects = await Subject.find({ teacherId }).sort({ code: 1 }).populate(POPULATE_TEACHER);
  if (subjects.length === 0) {
    return res.json({ subjects: [], students: [] });
  }

  const subjectIds = subjects.map((s) => s._id);
  const pairConditions = [];
  const seenPairs = new Set();
  for (const subject of subjects) {
    const key = `${subject.departmentId}|${subject.semesterId}`;
    if (seenPairs.has(key)) continue;
    seenPairs.add(key);
    pairConditions.push({ departmentId: subject.departmentId, currentSemesterId: subject.semesterId });
  }

  const students = await User.find({
    role: UserRole.STUDENT,
    $or: [...pairConditions, { retakeSubjectIds: { $in: subjectIds } }],
  }).sort({ fullName: 1 });

  const studentCountBySubject = new Map(subjects.map((s) => [s._id.toString(), 0]));
  const studentsJson = students.map((student) => {
    const matchedSubjectIds = [];
    for (const subject of subjects) {
      const isCurrentCohort = student.departmentId
        && student.currentSemesterId
        && student.departmentId.toString() === subject.departmentId.toString()
        && student.currentSemesterId.toString() === subject.semesterId.toString();
      const isRetaking = (student.retakeSubjectIds || []).some((id) => id.toString() === subject._id.toString());
      if (isCurrentCohort || isRetaking) {
        const subjectKey = subject._id.toString();
        matchedSubjectIds.push(subjectKey);
        studentCountBySubject.set(subjectKey, studentCountBySubject.get(subjectKey) + 1);
      }
    }
    return { ...student.toPublicJSON(), subjectIds: matchedSubjectIds };
  });

  const subjectsJson = subjects.map((subject) => ({
    ...subject.toObject({ versionKey: false }),
    studentCount: studentCountBySubject.get(subject._id.toString()),
  }));

  res.json({ subjects: subjectsJson, students: studentsJson });
});

/** The inverse of roster(): a Student's own subject list (current department+semester,
 * unioned with anything in their retakeSubjectIds) - self-scoped, no ownership checks
 * beyond being the student in question. */
const mine = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.STUDENT) {
    throw new ApiError(403, 'Only students have a personal subject list');
  }
  const subjects = await Subject.find({
    $or: [
      { departmentId: req.user.departmentId, semesterId: req.user.currentSemesterId },
      { _id: { $in: req.user.retakeSubjectIds || [] } },
    ],
  }).sort({ code: 1 }).populate(POPULATE_TEACHER);
  res.json({ subjects });
});

module.exports = { listForSemester, listForDepartment, listForTeacher, create, update, remove, assignTeacher, selfAssign, roster, myStudents, mine };
