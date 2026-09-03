const mongoose = require('mongoose');
const Promotion = require('../models/Promotion');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const Semester = require('../models/Semester');
const User = require('../models/User');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { isHodOfDepartment } = require('../services/authorization');
const { UserRole, PromotionStatus, ResultStatus, MAX_ALLOWED_FAILED_SUBJECTS, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse } = require('../services/notificationService');

/**
 * All pre-checks (spec §10) are enforced here regardless of what the UI already validated:
 * current semester fully APPROVED, fewer than 4 failed subjects, and not already at the
 * department's final configured semester. Only HOD (of the student's department) or Admin
 * may call this - a Teacher hitting it gets a 403, not just a hidden button client-side.
 */
const promote = asyncHandler(async (req, res) => {
  const student = await User.findById(req.params.id);
  if (!student || student.role !== UserRole.STUDENT) {
    throw new ApiError(404, 'Student not found');
  }
  if (req.user.role !== UserRole.ADMIN && !isHodOfDepartment(req.user, student.departmentId)) {
    throw new ApiError(403, 'Only the HOD of this student\'s department or an Admin can promote');
  }
  if (!student.currentSemesterId) {
    throw new ApiError(400, 'Student has no current semester set');
  }

  const currentSemester = await Semester.findById(student.currentSemesterId);
  if (!currentSemester) throw new ApiError(400, 'Current semester no longer exists');

  const semesterResult = await StudentSemesterResult.findOne({
    studentId: student._id,
    semesterId: currentSemester._id,
  });
  if (!semesterResult || semesterResult.resultStatus !== ResultStatus.APPROVED) {
    throw new ApiError(400, 'All results for the current semester must be APPROVED before promotion');
  }
  if (semesterResult.failedSubjectIds.length >= MAX_ALLOWED_FAILED_SUBJECTS) {
    throw new ApiError(403, 'NOT PROMOTED — 4 or more subjects failed');
  }

  const finalSemester = await Semester.findOne({ departmentId: student.departmentId }).sort({ number: -1 });
  if (!finalSemester || currentSemester.number >= finalSemester.number) {
    throw new ApiError(400, 'Student is already at the final semester for this department');
  }

  const nextSemester = await Semester.findOne({
    departmentId: student.departmentId,
    number: currentSemester.number + 1,
  });
  if (!nextSemester) {
    throw new ApiError(400, `Semester ${currentSemester.number + 1} has not been set up for this department yet`);
  }

  student.currentSemesterId = nextSemester._id;
  const existingRetakes = (student.retakeSubjectIds || []).map((id) => id.toString());
  const newRetakes = semesterResult.failedSubjectIds.map((id) => id.toString());
  student.retakeSubjectIds = Array.from(new Set([...existingRetakes, ...newRetakes]));

  semesterResult.promotionStatus = PromotionStatus.PROMOTED;

  // Promotion record + student's currentSemesterId + the old semester's promotionStatus
  // all move together, so a mid-sequence failure can't leave the student advanced without
  // a Promotion audit record, or vice versa.
  let promotion;
  const session = await mongoose.startSession();
  try {
    await session.withTransaction(async () => {
      try {
        promotion = await Promotion.create([{
          studentId: student._id,
          fromSemesterId: currentSemester._id,
          toSemesterId: nextSemester._id,
          promotedBy: req.user._id.toString(),
          failedSubjectIds: semesterResult.failedSubjectIds,
        }], { session });
        promotion = promotion[0];
      } catch (err) {
        if (err.code === 11000) {
          throw new ApiError(409, 'This student has already been promoted from this semester');
        }
        throw err;
      }

      await student.save({ session });
      await semesterResult.save({ session });
    });
  } finally {
    await session.endSession();
  }

  res.json({ promotion, student: student.toPublicJSON() });

  notifyAfterResponse(async () => {
    await notifyUsers([student._id.toString()], {
      type: NotificationType.STUDENT_PROMOTED,
      title: `Promoted to Semester ${nextSemester.number}`,
      body: `From Semester ${currentSemester.number}`,
      actorId: req.user._id.toString(),
      relatedType: 'promotion',
      relatedId: promotion._id.toString(),
    });
  });
});

/** Single aggregated payload (spec §11) rather than making the Android client stitch
 * together N+1 calls. */
const dashboard = asyncHandler(async (req, res) => {
  const student = await User.findById(req.params.id);
  if (!student || student.role !== UserRole.STUDENT) {
    throw new ApiError(404, 'Student not found');
  }
  const isSelf = req.user._id.toString() === student._id.toString();
  if (!isSelf && req.user.role !== UserRole.ADMIN && !isHodOfDepartment(req.user, student.departmentId)) {
    throw new ApiError(403, 'Not authorized to view this dashboard');
  }

  const semesterResults = await StudentSemesterResult.find({ studentId: student._id }).populate('semesterId');
  semesterResults.sort((a, b) => (a.semesterId?.number || 0) - (b.semesterId?.number || 0));

  const approved = semesterResults.filter((r) => r.resultStatus === ResultStatus.APPROVED);
  const allApprovedSubjects = approved.flatMap((r) => r.subjectResults);
  const totalCredits = allApprovedSubjects.reduce((sum, s) => sum + s.creditHours, 0);
  const weightedSum = allApprovedSubjects.reduce((sum, s) => sum + s.gpa * s.creditHours, 0);
  const cumulativeCGPA = totalCredits === 0 ? 0 : Math.round((weightedSum / totalCredits) * 100) / 100;

  const currentSemesterResult = student.currentSemesterId
    ? semesterResults.find((r) => r.semesterId && r.semesterId._id.toString() === student.currentSemesterId.toString())
    : null;

  res.json({
    identity: {
      id: student._id,
      fullName: student.fullName,
      registrationNumber: student.registrationNumber,
      rollNumber: student.rollNumber,
      departmentId: student.departmentId,
      sessionId: student.sessionId,
      currentSemesterId: student.currentSemesterId,
    },
    summary: {
      currentGPA: currentSemesterResult ? currentSemesterResult.semesterGPA : 0,
      cumulativeCGPA,
      totalSubjects: allApprovedSubjects.length,
      passedSubjects: allApprovedSubjects.filter((s) => s.status === 'PASS').length,
      failedSubjects: allApprovedSubjects.filter((s) => s.status === 'FAIL').length,
      completedSemesters: approved.length,
      promotionStatus: currentSemesterResult ? currentSemesterResult.promotionStatus : PromotionStatus.NOT_EVALUATED,
    },
    timeline: semesterResults.map((r) => ({
      semesterNumber: r.semesterId ? r.semesterId.number : null,
      gpa: r.semesterGPA,
      resultStatus: r.resultStatus,
      failedSubjects: r.failedSubjectIds,
      promotionStatus: r.promotionStatus,
    })),
  });
});

module.exports = { promote, dashboard };
