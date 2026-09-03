const ExamResult = require('../models/ExamResult');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const Subject = require('../models/Subject');
const Semester = require('../models/Semester');
const User = require('../models/User');
const { ResultStatus, PromotionStatus, MAX_ALLOWED_FAILED_SUBJECTS } = require('../constants/enums');

function round2(value) {
  return Math.round(value * 100) / 100;
}

function weightedGpa(subjectResults) {
  const totalCredits = subjectResults.reduce((sum, s) => sum + s.creditHours, 0);
  const weightedSum = subjectResults.reduce((sum, s) => sum + s.gpa * s.creditHours, 0);
  return { totalCredits, gpa: totalCredits === 0 ? 0 : round2(weightedSum / totalCredits) };
}

/**
 * The single reusable recalculation function the spec (§5) requires be called from every
 * mutation path that can change a student's grades: submit, approve, reject-then-resubmit,
 * a correction to an already-approved result, and repeat-exam approval. Rebuilds the
 * StudentSemesterResult document for (studentId, semesterId) entirely from the current
 * ExamResult rows - never patched piecemeal elsewhere.
 */
async function recalculateSemesterGpa(studentId, semesterId, { session } = {}) {
  const semester = await Semester.findById(semesterId).session(session);
  if (!semester) throw new Error(`Semester ${semesterId} not found`);

  const totalSubjectsInSemester = await Subject.countDocuments({ semesterId }).session(session);

  const approvedResults = await ExamResult.find({
    studentId,
    semesterId,
    resultStatus: ResultStatus.APPROVED,
  }).session(session).populate('subjectId');

  const subjectResults = approvedResults
    .filter((r) => r.subjectId) // defensive: skip if the subject was since deleted
    .map((r) => ({
      subjectId: r.subjectId._id,
      courseCode: r.subjectId.code,
      subjectName: r.subjectId.title,
      creditHours: r.subjectId.creditHours,
      marks: r.marks,
      grade: r.grade,
      gpa: r.gpa,
      status: r.status,
    }));

  const { gpa: semesterGPA } = weightedGpa(subjectResults);
  const failedSubjectIds = subjectResults.filter((s) => s.status === 'FAIL').map((s) => s.subjectId);

  const anyPending = await ExamResult.exists({
    studentId,
    semesterId,
    resultStatus: ResultStatus.PENDING_HOD_APPROVAL,
  }).session(session);

  let resultStatus;
  if (totalSubjectsInSemester > 0 && approvedResults.length >= totalSubjectsInSemester) {
    resultStatus = ResultStatus.APPROVED;
  } else if (anyPending || approvedResults.length > 0) {
    resultStatus = ResultStatus.PENDING_HOD_APPROVAL;
  } else {
    resultStatus = ResultStatus.DRAFT;
  }

  let promotionStatus = PromotionStatus.NOT_EVALUATED;
  if (resultStatus === ResultStatus.APPROVED) {
    promotionStatus = failedSubjectIds.length >= MAX_ALLOWED_FAILED_SUBJECTS
      ? PromotionStatus.NOT_PROMOTED
      : PromotionStatus.ELIGIBLE_FOR_PROMOTION;
  }

  // "Previous GPA" is a simple (studentId, semesterNumber - 1) lookup, not a value pushed
  // forward proactively - a later correction to semester N-1 is picked up automatically
  // the next time semester N is recalculated.
  let previousSemesterGPA = null;
  const previousSemester = await Semester.findOne({
    departmentId: semester.departmentId,
    number: semester.number - 1,
  });
  if (previousSemester) {
    const previousResult = await StudentSemesterResult.findOne({ studentId, semesterId: previousSemester._id }).session(session);
    if (previousResult) previousSemesterGPA = previousResult.semesterGPA;
  }

  const student = await User.findById(studentId).session(session);

  const saved = await StudentSemesterResult.findOneAndUpdate(
    { studentId, semesterId },
    {
      studentId,
      departmentId: semester.departmentId,
      semesterId,
      sessionId: student ? student.sessionId : null,
      subjectResults,
      semesterGPA,
      previousSemesterGPA,
      resultStatus,
      promotionStatus,
      failedSubjectIds,
    },
    { new: true, upsert: true, setDefaultsOnInsert: true, session }
  );

  // CGPA: credit-hour weighted across every APPROVED semester for this student (spec §5) -
  // recomputed after the upsert above so this semester's own just-changed status is
  // reflected if it just became (or stopped being) APPROVED.
  const approvedSemesters = await StudentSemesterResult.find({ studentId, resultStatus: ResultStatus.APPROVED }).session(session);
  const allApprovedSubjectResults = approvedSemesters.flatMap((s) => s.subjectResults);
  const { gpa: cumulativeCGPA } = weightedGpa(allApprovedSubjectResults);

  saved.cumulativeCGPA = cumulativeCGPA;
  await saved.save({ session });

  return saved;
}

module.exports = { recalculateSemesterGpa };
