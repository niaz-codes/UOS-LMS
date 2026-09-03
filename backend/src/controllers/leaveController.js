const LeaveApplication = require('../models/LeaveApplication');
const Media = require('../models/Media');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment, isTeacherInDepartment } = require('../services/authorization');
const {
  notifyUsers,
  notifyAfterResponse,
  resolveHodOfDepartment,
  resolveTeachersInDepartment,
  resolveAllAdmins,
} = require('../services/notificationService');

const POPULATE = [
  { path: 'studentId', select: 'fullName rollNumber registrationNumber' },
  { path: 'reviewerId', select: 'fullName' },
];

const apply = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.STUDENT) {
    throw new ApiError(403, 'Only a Student can apply for leave');
  }
  const { fromDate, toDate, reason, mediaId } = req.body;
  if (!fromDate || !toDate || !reason) {
    throw new ApiError(400, 'fromDate, toDate, and reason are required');
  }
  if (!req.user.departmentId) {
    throw new ApiError(400, 'You are not assigned to a department yet');
  }

  const fields = {
    studentId: req.user._id.toString(),
    departmentId: req.user.departmentId,
    semesterId: req.user.currentSemesterId || null,
    fromDate,
    toDate,
    reason,
  };

  if (mediaId) {
    const media = await Media.findById(mediaId);
    if (media) {
      fields.mediaId = media._id;
      fields.attachmentUrl = media.secureUrl;
      fields.attachmentName = media.fileName;
      fields.attachmentPublicId = media.publicId;
      fields.attachmentResourceType = media.resourceType;
      fields.attachmentSize = media.fileSize;
    }
  }

  const leave = await LeaveApplication.create(fields);
  await leave.populate(POPULATE);
  res.status(201).json({ leave });

  notifyAfterResponse(async () => {
    const [teachers, hod, admins] = await Promise.all([
      resolveTeachersInDepartment(leave.departmentId),
      resolveHodOfDepartment(leave.departmentId),
      resolveAllAdmins(),
    ]);
    await notifyUsers([...teachers, ...hod, ...admins], {
      type: NotificationType.LEAVE_APPLIED,
      title: `Leave request: ${req.user.fullName}`,
      body: reason,
      actorId: req.user._id.toString(),
      relatedType: 'leaveApplication',
      relatedId: leave._id.toString(),
    });
  });
});

/** Student sees their own full history; Teacher/HOD see their department's queue
 * (optionally filtered to status=PENDING); Admin sees everything. */
const list = asyncHandler(async (req, res) => {
  const { status } = req.query;
  const filter = {};

  if (req.user.role === UserRole.STUDENT) {
    filter.studentId = req.user._id.toString();
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
  } else if (req.user.role === UserRole.TEACHER) {
    filter.departmentId = { $in: req.user.departmentIds || [] };
  }
  // Admin: unfiltered.

  if (status) filter.status = status;

  const leaves = await LeaveApplication.find(filter).sort({ createdAt: -1 }).populate(POPULATE);
  res.json({ leaves });
});

/** "First responder wins": any Teacher/HOD of the student's department, or an Admin, may
 * decide a PENDING leave - the findOneAndUpdate's status:'PENDING' filter is the
 * concurrency guard (a second decider's write simply matches zero documents and 409s),
 * replacing the pre-migration app's Firestore-rules-level equivalent. */
async function decide(req, res, status) {
  const leave = await LeaveApplication.findById(req.params.id);
  if (!leave) throw new ApiError(404, 'Leave application not found');

  const authorized = req.user.role === UserRole.ADMIN
    || isHodOfDepartment(req.user, leave.departmentId)
    || isTeacherInDepartment(req.user, leave.departmentId);
  if (!authorized) {
    throw new ApiError(403, 'Not authorized to decide this leave application');
  }

  const updated = await LeaveApplication.findOneAndUpdate(
    { _id: leave._id, status: 'PENDING' },
    { status, reviewerId: req.user._id.toString(), reviewerRole: req.user.role, decidedAt: new Date() },
    { new: true }
  ).populate(POPULATE);

  if (!updated) {
    throw new ApiError(409, 'This leave application has already been decided');
  }
  res.json({ leave: updated });

  notifyAfterResponse(async () => {
    await notifyUsers([updated.studentId._id ? updated.studentId._id.toString() : updated.studentId.toString()], {
      type: status === 'APPROVED' ? NotificationType.LEAVE_APPROVED : NotificationType.LEAVE_REJECTED,
      title: `Leave ${status === 'APPROVED' ? 'approved' : 'rejected'}`,
      body: updated.reason,
      actorId: req.user._id.toString(),
      relatedType: 'leaveApplication',
      relatedId: updated._id.toString(),
    });
  });
}

const approve = asyncHandler((req, res) => decide(req, res, 'APPROVED'));
const reject = asyncHandler((req, res) => decide(req, res, 'REJECTED'));

module.exports = { apply, list, approve, reject };
