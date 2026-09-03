const Department = require('../models/Department');
const Semester = require('../models/Semester');
const Session = require('../models/Session');
const Subject = require('../models/Subject');
const User = require('../models/User');
const ExamResult = require('../models/ExamResult');
const RepeatExam = require('../models/RepeatExam');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole } = require('../constants/enums');

// Every result row is returned with just enough populated subject/student/teacher fields
// that the client's spreadsheet can render without N+1 lookups, reusing the same ref shapes
// the other exam-result endpoints already serialize (ExamResultResponseDto on the client).
const POPULATE_SUBJECT = { path: 'subjectId', select: 'code title creditHours departmentId semesterId' };
const POPULATE_STUDENT = { path: 'studentId', select: 'fullName rollNumber registrationNumber' };
const POPULATE_TEACHER = { path: 'submittedBy', select: 'fullName' };
const POPULATE_RESULT = [POPULATE_SUBJECT, POPULATE_STUDENT, POPULATE_TEACHER];

const EXAM_TYPES = ['current', 'repeat', 'previous'];

/** The Teacher's Exam Result workspace. Everything is derived from the subjects the teacher
 * owns (Subject.teacherId == me), never from the whole university:
 *  - departments: the departments those subjects belong to;
 *  - semesters: the curriculum semesters those subjects are taught in;
 *  - sessions: only the cohorts (Student.sessionId) that actually have students in those
 *    subjects - either currently placed in the department+semester or retaking one of the
 *    teacher's subjects. The client renders Department -> Session -> Semester tabs from this. */
const getOptions = asyncHandler(async (req, res) => {
  const subjects = await Subject.find({ teacherId: req.user._id.toString() }).select('departmentId semesterId').lean();
  if (subjects.length === 0) return res.json({ departments: [] });

  const departmentIds = [...new Set(subjects.map((s) => s.departmentId.toString()))];
  const semesterIds = [...new Set(subjects.map((s) => s.semesterId.toString()))];

  const departments = await Department.find({ _id: { $in: departmentIds } }).sort({ name: 1 }).lean();
  const semesters = await Semester.find({ _id: { $in: semesterIds } }).sort({ number: 1 }).lean();

  const semestersByDept = new Map();
  for (const semester of semesters) {
    const key = semester.departmentId.toString();
    if (!semestersByDept.has(key)) semestersByDept.set(key, []);
    semestersByDept.get(key).push(semester);
  }

  const students = await User.find({
    role: UserRole.STUDENT,
    $or: [
      { departmentId: { $in: departmentIds }, currentSemesterId: { $in: semesterIds } },
      { retakeSubjectIds: { $in: subjects.map((s) => s._id) } },
    ],
  }).select('departmentId sessionId').lean();

  const sessionIds = [...new Set(
    students.map((s) => (s.sessionId ? s.sessionId.toString() : null)).filter(Boolean)
  )];
  const sessions = sessionIds.length > 0
    ? await Session.find({ _id: { $in: sessionIds } }).sort({ label: 1 }).lean()
    : [];

  const sessionsByDept = new Map();
  for (const session of sessions) {
    const key = session.departmentId.toString();
    if (!sessionsByDept.has(key)) sessionsByDept.set(key, []);
    sessionsByDept.get(key).push(session);
  }

  const result = departments.map((dept) => ({
    _id: dept._id,
    name: dept.name,
    code: dept.code,
    sessions: sessionsByDept.get(dept._id.toString()) || [],
    semesters: semestersByDept.get(dept._id.toString()) || [],
  }));

  res.json({ departments: result });
});

/** The teacher's own subjects for one (department, semester) that actually have students in
 * the chosen session - current-cohort students take every subject of the pair, retaking
 * students only the subjects in their retakeSubjectIds. */
const listSubjects = asyncHandler(async (req, res) => {
  const { departmentId, semesterId, sessionId } = req.query;
  if (!departmentId || !semesterId || !sessionId) {
    throw new ApiError(400, 'departmentId, semesterId, and sessionId are required');
  }

  const subjects = await Subject.find({
    teacherId: req.user._id.toString(),
    departmentId,
    semesterId,
  }).sort({ code: 1 }).lean();
  if (subjects.length === 0) return res.json({ subjects: [] });

  const subjectIds = subjects.map((s) => s._id);
  const students = await User.find({
    role: UserRole.STUDENT,
    sessionId,
    $or: [
      { departmentId, currentSemesterId: semesterId },
      { retakeSubjectIds: { $in: subjectIds } },
    ],
  }).select('departmentId currentSemesterId retakeSubjectIds').lean();

  const retakeIds = new Set();
  for (const student of students) {
    if (student.retakeSubjectIds) {
      for (const id of student.retakeSubjectIds) retakeIds.add(id.toString());
    }
  }
  const hasCurrentCohort = students.some(
    (s) => s.departmentId && s.departmentId.toString() === departmentId
      && s.currentSemesterId && s.currentSemesterId.toString() === semesterId
  );

  const eligible = subjects.filter(
    (s) => hasCurrentCohort || retakeIds.has(s._id.toString())
  );
  res.json({ subjects: eligible });
});

/** Marksheet rows for one subject (+ optional session filter) per exam type:
 *  - current:  the full roster, joined with the student's existing ExamResult (null if none);
 *  - repeat:   only repeat-eligible students (failed, no approved repeat yet), joined with the
 *              latest RepeatExam so previous/new marks and review status are visible;
 *  - previous: only students who already have a result - read-only, though a DRAFT/REJECTED
 *              row is editable so the teacher can re-submit from this same screen.
 * The client decides editability from the result's resultStatus; marks are the 0-100 values
 * the rest of the grading pipeline already uses. */
const listRoster = asyncHandler(async (req, res) => {
  const { subjectId, sessionId, examType } = req.query;
  if (!subjectId || !examType || !EXAM_TYPES.includes(examType)) {
    throw new ApiError(400, 'subjectId and examType (current|repeat|previous) are required');
  }

  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');
  if (subject.teacherId !== req.user._id.toString()) {
    throw new ApiError(403, 'Only this subject\'s assigned teacher can manage its results');
  }

  const roster = await User.find({
    role: UserRole.STUDENT,
    $or: [
      { departmentId: subject.departmentId, currentSemesterId: subject.semesterId },
      { retakeSubjectIds: subject._id },
    ],
  }).sort({ fullName: 1 });

  let students = roster;
  if (sessionId) {
    students = roster.filter((s) => s.sessionId && s.sessionId.toString() === sessionId);
  }

  const studentIds = students.map((s) => s._id.toString());
  const results = await ExamResult.find({ subjectId, studentId: { $in: studentIds } })
    .sort({ updatedAt: -1 })
    .populate(POPULATE_RESULT)
    .lean();

  const resultByStudent = new Map(results.map((r) => [r.studentId._id.toString(), r]));

  if (examType === 'repeat') {
    const resultDocs = [...resultByStudent.values()];
    const repeatExams = resultDocs.length > 0
      ? await RepeatExam.find({ examResultId: { $in: resultDocs.map((r) => r._id) } })
          .sort({ createdAt: -1 })
          .populate([POPULATE_SUBJECT, POPULATE_STUDENT])
          .lean()
      : [];
    const latestRepeatByResult = new Map();
    for (const repeat of repeatExams) {
      if (!latestRepeatByResult.has(repeat.examResultId.toString())) {
        latestRepeatByResult.set(repeat.examResultId.toString(), repeat);
      }
    }

    const rows = students
      .filter((s) => {
        const result = resultByStudent.get(s._id.toString());
        return result && result.repeatEligible;
      })
      .map((s) => {
        const result = resultByStudent.get(s._id.toString());
        return {
          student: s.toPublicJSON(),
          result,
          repeatExam: latestRepeatByResult.get(result._id.toString()) || null,
        };
      });
    return res.json({ rows });
  }

  if (examType === 'previous') {
    const rows = students
      .filter((s) => resultByStudent.has(s._id.toString()))
      .map((s) => ({
        student: s.toPublicJSON(),
        result: resultByStudent.get(s._id.toString()),
        repeatExam: null,
      }));
    return res.json({ rows });
  }

  const rows = students.map((s) => ({
    student: s.toPublicJSON(),
    result: resultByStudent.get(s._id.toString()) || null,
    repeatExam: null,
  }));
  res.json({ rows });
});

module.exports = { getOptions, listSubjects, listRoster };
