const TeacherAttendanceRecord = require('../models/TeacherAttendanceRecord');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole } = require('../constants/enums');

const POPULATE_TEACHER = { path: 'teacherId', select: 'fullName' };
const POPULATE_SUBJECT = { path: 'subjectId', select: 'code title' };

/** Every subjectId in the roster must belong to departmentId and semesterId, and must have a
 * teacher assigned - the teacherId on each saved record is always resolved from the subject
 * itself (never trusted from the client), so a HOD can't spoof attendance against a teacher
 * who isn't actually that course's assigned teacher. Returns a Map<subjectId, Subject>. */
async function requireSubjectsInScope(subjectIds, departmentId, semesterId) {
  const unique = [...new Set(subjectIds)];
  const subjects = await Subject.find({ _id: { $in: unique }, departmentId, semesterId });
  if (subjects.length !== unique.length) {
    throw new ApiError(403, 'One or more courses do not belong to this department/semester');
  }
  const bySubjectId = new Map();
  for (const subject of subjects) {
    if (!subject.teacherId) {
      throw new ApiError(400, `Course ${subject.code} has no teacher assigned yet`);
    }
    bySubjectId.set(subject._id.toString(), subject);
  }
  return bySubjectId;
}

/** Upserts a whole course-list's worth of attendance for one (semester, dateKey) day - one
 * record per (subject, dateKey), teacherId always resolved server-side from the subject so a
 * teacher's status is genuinely tied to the course they teach, not client-asserted. HOD is
 * always forced to their own department (client-supplied departmentId ignored), Admin may
 * target any department explicitly. */
const save = asyncHandler(async (req, res) => {
  const { semesterId, dateKey, records } = req.body;
  const departmentId = req.user.role === UserRole.HOD ? req.user.departmentId : req.body.departmentId;

  if (!departmentId || !semesterId || !dateKey) {
    throw new ApiError(400, 'departmentId, semesterId, and dateKey are required');
  }
  if (!Array.isArray(records) || records.length === 0) {
    throw new ApiError(400, 'records must be a non-empty array of { subjectId, status }');
  }
  for (const r of records) {
    if (!r.subjectId || !['PRESENT', 'ABSENT'].includes(r.status)) {
      throw new ApiError(400, 'Each record requires a subjectId and status of PRESENT or ABSENT');
    }
  }

  const subjectsById = await requireSubjectsInScope(records.map((r) => r.subjectId), departmentId, semesterId);

  const saved = [];
  for (const r of records) {
    const subject = subjectsById.get(r.subjectId);
    const doc = await TeacherAttendanceRecord.findOneAndUpdate(
      { subjectId: r.subjectId, dateKey },
      {
        teacherId: subject.teacherId,
        subjectId: r.subjectId,
        departmentId,
        semesterId,
        dateKey,
        status: r.status,
        markedBy: req.user._id.toString(),
      },
      { new: true, upsert: true, setDefaultsOnInsert: true }
    ).populate(POPULATE_TEACHER).populate(POPULATE_SUBJECT);
    saved.push(doc);
  }

  res.json({ records: saved });
});

/** Role-scoped: Admin sees everything (+ optional department/teacher/subject/semester/month/
 * status filters); HOD is forced to their own department; Teacher is forced to their own
 * records (view-only - no write access, enforced at the route level via requireRole on save). */
const list = asyncHandler(async (req, res) => {
  const { departmentId, teacherId, subjectId, semesterId, dateKey, month, status } = req.query;
  const filter = {};

  if (req.user.role === UserRole.ADMIN) {
    if (departmentId) filter.departmentId = departmentId;
    if (teacherId) filter.teacherId = teacherId;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
    if (teacherId) filter.teacherId = teacherId;
  } else {
    filter.teacherId = req.user._id.toString();
  }

  if (subjectId) filter.subjectId = subjectId;
  if (semesterId) filter.semesterId = semesterId;
  if (dateKey) filter.dateKey = dateKey;
  else if (month) filter.dateKey = { $regex: `^${month}` };
  if (status) filter.status = status;

  const records = await TeacherAttendanceRecord.find(filter)
      .sort({ dateKey: -1 })
      .populate(POPULATE_TEACHER)
      .populate(POPULATE_SUBJECT);
  res.json({ records });
});

module.exports = { save, list };
