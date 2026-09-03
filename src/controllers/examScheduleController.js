const ExamSchedule = require('../models/ExamSchedule');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse, resolveStudentsInCohort } = require('../services/notificationService');

const POPULATE = [
  { path: 'subjectId', select: 'code title' },
  { path: 'teacherId', select: 'fullName' },
  { path: 'invigilatorId', select: 'fullName' },
];

function normalizeRoom(room) {
  return room.trim().toLowerCase().replace(/\s+/g, '_');
}

function dateKeyOf(date) {
  return new Date(date).toISOString().slice(0, 10);
}

const create = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can create exam schedules');
  }
  const { subjectId, examType, examDate, startTimeMinutes, endTimeMinutes, room, invigilatorId } = req.body;
  if (!subjectId || !examDate || startTimeMinutes == null || endTimeMinutes == null || !room) {
    throw new ApiError(400, 'subjectId, examDate, startTimeMinutes, endTimeMinutes, and room are required');
  }
  if (endTimeMinutes <= startTimeMinutes) {
    throw new ApiError(400, 'endTimeMinutes must be after startTimeMinutes');
  }

  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');

  const roomNormalized = normalizeRoom(room);
  const examDateKey = dateKeyOf(examDate);

  const [invigilatorClash, roomClash] = await Promise.all([
    invigilatorId
      ? ExamSchedule.findOne({ invigilatorId, examDateKey, startTimeMinutes })
      : Promise.resolve(null),
    ExamSchedule.findOne({ roomNormalized, examDateKey, startTimeMinutes }),
  ]);
  if (invigilatorClash) throw new ApiError(409, 'This invigilator already has an exam scheduled at this date and time.');
  if (roomClash) throw new ApiError(409, 'This room is already booked for an exam at this date and time.');

  let schedule;
  try {
    schedule = await ExamSchedule.create({
      examType: examType === 'FINAL_TERM' ? 'FINAL_TERM' : 'MID_TERM',
      subjectId,
      departmentId: subject.departmentId,
      semesterId: subject.semesterId,
      teacherId: subject.teacherId || null,
      invigilatorId: invigilatorId || null,
      room,
      roomNormalized,
      examDate,
      examDateKey,
      startTimeMinutes,
      endTimeMinutes,
      createdBy: req.user._id.toString(),
    });
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, 'This exam schedule conflicts with an existing one.');
    throw err;
  }
  await schedule.populate(POPULATE);
  res.status(201).json({ schedule });
});

/** Admin: everything (+ optional departmentId filter). HOD: own department, every status.
 * Teacher: their own subject OR invigilator assignments, every status. Student: own
 * cohort, PUBLISHED/LOCKED only. */
const list = asyncHandler(async (req, res) => {
  const { departmentId, semesterId } = req.query;
  let filter = {};

  if (req.user.role === UserRole.ADMIN) {
    if (departmentId) filter.departmentId = departmentId;
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
  } else if (req.user.role === UserRole.TEACHER) {
    filter = { $or: [{ teacherId: req.user._id.toString() }, { invigilatorId: req.user._id.toString() }] };
  } else {
    if (!req.user.departmentId || !req.user.currentSemesterId) {
      return res.json({ schedules: [] });
    }
    filter = {
      departmentId: req.user.departmentId,
      semesterId: req.user.currentSemesterId,
      status: { $in: ['PUBLISHED', 'LOCKED'] },
    };
  }

  if (semesterId && req.user.role !== UserRole.STUDENT) filter.semesterId = semesterId;

  const schedules = await ExamSchedule.find(filter).sort({ examDate: 1, startTimeMinutes: 1 }).populate(POPULATE);
  res.json({ schedules });
});

async function transition(req, res, from, to, timestampField) {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can change an exam schedule\'s status');
  }
  const schedule = await ExamSchedule.findById(req.params.id);
  if (!schedule) throw new ApiError(404, 'Exam schedule not found');
  if (schedule.status !== from) {
    throw new ApiError(400, `Cannot move from ${schedule.status} to ${to}`);
  }
  schedule.status = to;
  schedule[timestampField] = new Date();
  await schedule.save();
  await schedule.populate(POPULATE);
  res.json({ schedule });

  if (to === 'PUBLISHED') {
    notifyAfterResponse(async () => {
      const recipientIds = await resolveStudentsInCohort(schedule.departmentId, schedule.semesterId);
      if (schedule.teacherId) recipientIds.push(schedule.teacherId);
      if (schedule.invigilatorId) recipientIds.push(schedule.invigilatorId);
      await notifyUsers(recipientIds, {
        type: NotificationType.EXAM_SCHEDULE_PUBLISHED,
        title: `Exam schedule published: ${schedule.subjectId && schedule.subjectId.title ? schedule.subjectId.title : ''}`,
        body: `${schedule.examType} - ${schedule.room}`,
        actorId: req.user._id.toString(),
        relatedType: 'examSchedule',
        relatedId: schedule._id.toString(),
      });
    });
  }
}

const publish = asyncHandler((req, res) => transition(req, res, 'DRAFT', 'PUBLISHED', 'publishedAt'));
const lock = asyncHandler((req, res) => transition(req, res, 'PUBLISHED', 'LOCKED', 'lockedAt'));

const remove = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can delete an exam schedule');
  }
  const schedule = await ExamSchedule.findByIdAndDelete(req.params.id);
  if (!schedule) throw new ApiError(404, 'Exam schedule not found');
  res.status(204).send();
});

module.exports = { create, list, publish, lock, remove };
