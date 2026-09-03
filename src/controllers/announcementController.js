const Announcement = require('../models/Announcement');
const Subject = require('../models/Subject');
const User = require('../models/User');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, UserStatus, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse, resolveStudentsInCohort, resolveAllApprovedUsers } = require('../services/notificationService');

const POPULATE = [
  { path: 'authorId', select: 'fullName' },
  { path: 'departmentId', select: 'name' },
  { path: 'subjectId', select: 'title code' },
];

/** Admin can post to any scope; HOD is locked to DEPARTMENT scope on their own department;
 * Teacher is locked to SUBJECT scope on a subject they teach. Matches the pre-migration
 * firestore.rules exactly. */
const create = asyncHandler(async (req, res) => {
  const { title, body, scope, departmentId, subjectId } = req.body;
  if (!title || !body) throw new ApiError(400, 'title and body are required');

  const fields = { title, body, authorId: req.user._id.toString() };

  if (req.user.role === UserRole.ADMIN) {
    fields.scope = scope || 'ALL';
    if (fields.scope === 'DEPARTMENT') {
      if (!departmentId) throw new ApiError(400, 'departmentId is required for DEPARTMENT scope');
      fields.departmentId = departmentId;
    } else if (fields.scope === 'SUBJECT') {
      if (!subjectId) throw new ApiError(400, 'subjectId is required for SUBJECT scope');
      fields.subjectId = subjectId;
    }
  } else if (req.user.role === UserRole.HOD) {
    if (!req.user.departmentId) throw new ApiError(403, 'You are not assigned to a department');
    fields.scope = 'DEPARTMENT';
    fields.departmentId = req.user.departmentId;
  } else if (req.user.role === UserRole.TEACHER) {
    if (!subjectId) throw new ApiError(400, 'subjectId is required');
    const subject = await Subject.findById(subjectId);
    if (!subject || subject.teacherId !== req.user._id.toString()) {
      throw new ApiError(403, 'You can only post to a subject you teach');
    }
    fields.scope = 'SUBJECT';
    fields.subjectId = subjectId;
  } else {
    throw new ApiError(403, 'Students cannot post announcements');
  }

  const announcement = await Announcement.create(fields);
  await announcement.populate(POPULATE);
  res.status(201).json({ announcement });

  notifyAfterResponse(async () => {
    const recipientIds = await resolveAnnouncementRecipients(announcement);
    await notifyUsers(recipientIds, {
      type: NotificationType.ANNOUNCEMENT_POSTED,
      title: announcement.title,
      body: announcement.body,
      actorId: req.user._id.toString(),
      relatedType: 'announcement',
      relatedId: announcement._id.toString(),
    });
  });
});

async function resolveAnnouncementRecipients(announcement) {
  if (announcement.scope === 'SUBJECT') {
    const subject = await Subject.findById(announcement.subjectId);
    return subject ? resolveStudentsInCohort(subject.departmentId, subject.semesterId) : [];
  }
  if (announcement.scope === 'DEPARTMENT') {
    const users = await User.find({
      status: UserStatus.APPROVED,
      $or: [{ departmentId: announcement.departmentId }, { departmentIds: announcement.departmentId }],
    }).select('_id').lean();
    return users.map((u) => u._id);
  }
  return resolveAllApprovedUsers();
}

const list = asyncHandler(async (req, res) => {
  const announcements = await Announcement.find().sort({ createdAt: -1 }).populate(POPULATE);
  res.json({ announcements });
});

const remove = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can delete an announcement');
  }
  const announcement = await Announcement.findByIdAndDelete(req.params.id);
  if (!announcement) throw new ApiError(404, 'Announcement not found');
  res.status(204).send();
});

module.exports = { create, list, remove };
