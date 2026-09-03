const CalendarEvent = require('../models/CalendarEvent');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse, resolveAllApprovedUsers } = require('../services/notificationService');

const create = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can create calendar events');
  }
  const { title, description, type, date } = req.body;
  if (!title || !date) throw new ApiError(400, 'title and date are required');

  const event = await CalendarEvent.create({
    title,
    description: description || '',
    type: ['HOLIDAY', 'EVENT', 'EXAM'].includes(type) ? type : 'EVENT',
    date,
    createdBy: req.user._id.toString(),
  });
  res.status(201).json({ event });

  notifyAfterResponse(async () => {
    const recipientIds = await resolveAllApprovedUsers();
    await notifyUsers(recipientIds, {
      type: NotificationType.CALENDAR_EVENT_ADDED,
      title: event.title,
      body: event.description,
      actorId: req.user._id.toString(),
      relatedType: 'calendarEvent',
      relatedId: event._id.toString(),
    });
  });
});

const list = asyncHandler(async (req, res) => {
  const events = await CalendarEvent.find().sort({ date: 1 });
  res.json({ events });
});

const remove = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can delete calendar events');
  }
  const event = await CalendarEvent.findByIdAndDelete(req.params.id);
  if (!event) throw new ApiError(404, 'Calendar event not found');
  res.status(204).send();
});

module.exports = { create, list, remove };
