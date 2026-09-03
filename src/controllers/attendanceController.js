const AttendanceRecord = require('../models/AttendanceRecord');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');
const { notifyUsers, notifyAfterResponse } = require('../services/notificationService');

const POPULATE_STUDENT = { path: 'studentId', select: 'fullName rollNumber registrationNumber' };

/** Upserts a whole roster's worth of records for one (subject, dateKey) session in one
 * call - mirrors the old app's WriteBatch semantics, just as a loop of upserts instead of
 * a single atomic batch (Mongo doesn't need the batch to get the same end state, since
 * each row is independently idempotent by the unique index). */
const save = asyncHandler(async (req, res) => {
  const { subjectId, departmentId, semesterId, dateKey, records } = req.body;
  if (!subjectId || !departmentId || !semesterId || !dateKey) {
    throw new ApiError(400, 'subjectId, departmentId, semesterId, and dateKey are required');
  }
  if (!Array.isArray(records) || records.length === 0) {
    throw new ApiError(400, 'records must be a non-empty array of { studentId, status }');
  }
  if (![UserRole.ADMIN, UserRole.TEACHER, UserRole.HOD].includes(req.user.role)) {
    throw new ApiError(403, 'Only a Teacher, HOD, or Admin can record attendance');
  }
  if (req.user.role === UserRole.HOD) {
    const subject = await Subject.findById(subjectId);
    if (!subject || !isHodOfDepartment(req.user, subject.departmentId)) {
      throw new ApiError(403, 'HOD can only record attendance for subjects in their own department');
    }
  }

  const saved = [];
  for (const r of records) {
    if (!r.studentId || !['PRESENT', 'ABSENT'].includes(r.status)) {
      throw new ApiError(400, 'Each record requires a studentId and status of PRESENT or ABSENT');
    }
    const doc = await AttendanceRecord.findOneAndUpdate(
      { subjectId, dateKey, studentId: r.studentId },
      { subjectId, departmentId, semesterId, dateKey, studentId: r.studentId, status: r.status, markedBy: req.user._id.toString() },
      { new: true, upsert: true, setDefaultsOnInsert: true }
    ).populate(POPULATE_STUDENT);
    saved.push(doc);
  }

  res.json({ records: saved });

  const absentStudentIds = saved.filter((r) => r.status === 'ABSENT').map((r) => r.studentId);
  if (absentStudentIds.length > 0) {
    notifyAfterResponse(async () => {
      const subject = await Subject.findById(subjectId);
      await notifyUsers(absentStudentIds, {
        type: NotificationType.ATTENDANCE_MARKED_ABSENT,
        title: `Marked absent: ${subject && (subject.title || subject.code) ? (subject.title || subject.code) : ''}`,
        body: dateKey,
        actorId: req.user._id.toString(),
        relatedType: 'attendanceRecord',
        relatedId: subjectId,
      });
    });
  }
});

/** Role-scoped: Admin sees everything (+ optional department/semester filters); HOD is
 * forced to their own department; Teacher must scope by subjectId (their own subject,
 * ownership isn't re-verified here since attendance read is less sensitive than write -
 * any teacher can see a subject's attendance history, matching the old rules which allowed
 * isTeacherOfSubject without restricting to their subjects for read); Student is forced to
 * their own records. */
const list = asyncHandler(async (req, res) => {
  const { subjectId, studentId, dateKey, departmentId, semesterId } = req.query;
  const filter = {};

  if (req.user.role === UserRole.ADMIN) {
    if (departmentId) filter.departmentId = departmentId;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
  } else if (req.user.role === UserRole.TEACHER) {
    if (!subjectId) throw new ApiError(400, 'subjectId is required');
  } else {
    filter.studentId = req.user._id.toString();
  }

  if (subjectId) filter.subjectId = subjectId;
  if (studentId && req.user.role !== UserRole.STUDENT) filter.studentId = studentId;
  if (dateKey) filter.dateKey = dateKey;
  if (semesterId) filter.semesterId = semesterId;

  const records = await AttendanceRecord.find(filter).sort({ dateKey: -1 }).populate(POPULATE_STUDENT);
  res.json({ records });
});

module.exports = { save, list };
