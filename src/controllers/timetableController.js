const TimetableSlot = require('../models/TimetableSlot');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse, resolveStudentsInCohort } = require('../services/notificationService');

async function notifyCohortAndTeacher(type, title, body, actorId, slot, relatedId) {
  const recipientIds = await resolveStudentsInCohort(slot.departmentId, slot.semesterId);
  if (slot.teacherId) recipientIds.push(slot.teacherId);
  await notifyUsers(recipientIds, {
    type,
    title,
    body,
    actorId,
    relatedType: 'timetableSlot',
    relatedId,
  });
}

const POPULATE = [
  { path: 'subjectId', select: 'code title' },
  { path: 'teacherId', select: 'fullName' },
];

function normalizeRoom(room) {
  return room.trim().toLowerCase().replace(/\s+/g, '_');
}

const create = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can create timetable slots');
  }
  const { subjectId, dayOfWeek, startTimeMinutes, endTimeMinutes, room } = req.body;
  if (!subjectId || !dayOfWeek || startTimeMinutes == null || endTimeMinutes == null || !room) {
    throw new ApiError(400, 'subjectId, dayOfWeek, startTimeMinutes, endTimeMinutes, and room are required');
  }
  if (endTimeMinutes <= startTimeMinutes) {
    throw new ApiError(400, 'endTimeMinutes must be after startTimeMinutes');
  }

  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');

  const roomNormalized = normalizeRoom(room);

  const [cohortClash, teacherClash, roomClash] = await Promise.all([
    TimetableSlot.findOne({ departmentId: subject.departmentId, semesterId: subject.semesterId, dayOfWeek, startTimeMinutes }),
    subject.teacherId
      ? TimetableSlot.findOne({ teacherId: subject.teacherId, dayOfWeek, startTimeMinutes })
      : Promise.resolve(null),
    TimetableSlot.findOne({ roomNormalized, dayOfWeek, startTimeMinutes }),
  ]);
  if (cohortClash) throw new ApiError(409, 'This department/semester already has a class at this day and time.');
  if (teacherClash) throw new ApiError(409, 'This teacher already has a class scheduled at this day and time.');
  if (roomClash) throw new ApiError(409, 'This room is already booked at this day and time.');

  let slot;
  try {
    slot = await TimetableSlot.create({
      departmentId: subject.departmentId,
      semesterId: subject.semesterId,
      subjectId,
      teacherId: subject.teacherId || null,
      dayOfWeek,
      startTimeMinutes,
      endTimeMinutes,
      room,
      roomNormalized,
      createdBy: req.user._id.toString(),
    });
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, 'This slot conflicts with an existing one.');
    throw err;
  }
  await slot.populate(POPULATE);
  res.status(201).json({ slot });

  notifyAfterResponse(() => notifyCohortAndTeacher(
    NotificationType.TIMETABLE_UPDATED,
    `New timetable slot: ${subject.title || subject.code || ''}`,
    `${room} - day ${dayOfWeek}`,
    req.user._id.toString(),
    slot,
    slot._id.toString()
  ));
});

/** Any approved user can read (role/scope narrows WHICH slots are relevant, not whether
 * they can read at all - matches the pre-migration rules exactly). */
const list = asyncHandler(async (req, res) => {
  const { departmentId, semesterId, teacherId } = req.query;
  const filter = {};

  if (req.user.role === UserRole.TEACHER && !departmentId && !teacherId) {
    filter.teacherId = req.user._id.toString();
  } else if (req.user.role === UserRole.STUDENT && !departmentId) {
    if (!req.user.departmentId || !req.user.currentSemesterId) {
      return res.json({ slots: [] });
    }
    filter.departmentId = req.user.departmentId;
    filter.semesterId = req.user.currentSemesterId;
  } else {
    if (departmentId) filter.departmentId = departmentId;
    if (semesterId) filter.semesterId = semesterId;
    if (teacherId) filter.teacherId = teacherId;
  }

  const slots = await TimetableSlot.find(filter).sort({ dayOfWeek: 1, startTimeMinutes: 1 }).populate(POPULATE);
  res.json({ slots });
});

const remove = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can delete timetable slots');
  }
  const slot = await TimetableSlot.findById(req.params.id).populate(POPULATE);
  if (!slot) throw new ApiError(404, 'Slot not found');
  await slot.deleteOne();
  res.status(204).send();

  notifyAfterResponse(() => notifyCohortAndTeacher(
    NotificationType.TIMETABLE_SLOT_REMOVED,
    `Timetable slot removed: ${slot.subjectId && slot.subjectId.title ? slot.subjectId.title : ''}`,
    `${slot.room} - day ${slot.dayOfWeek}`,
    req.user._id.toString(),
    slot,
    slot._id.toString()
  ));
});

module.exports = { create, list, remove };
