const TeacherLeaveApplication = require('../models/TeacherLeaveApplication');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');
const { notifyUsers, notifyAfterResponse, resolveHodOfDepartment, resolveAllAdmins } = require('../services/notificationService');

const LEAVE_TYPES = ['CASUAL', 'SICK', 'ANNUAL', 'OTHER'];

const POPULATE = [
  { path: 'teacherId', select: 'fullName employeeId' },
  { path: 'reviewerId', select: 'fullName' },
];

/** A Teacher can belong to several departments (User.departmentIds); the leave request needs
 * exactly one to route to a HOD. Prefers the legacy scalar departmentId if set (mirrors
 * services/authorization.isTeacherInDepartment's own precedence), else the first joined one. */
function resolveTeacherDepartment(user) {
  if (user.departmentId) return user.departmentId;
  if (user.departmentIds && user.departmentIds.length > 0) return user.departmentIds[0];
  return null;
}

const apply = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.TEACHER) {
    throw new ApiError(403, 'Only a Teacher can apply for this leave');
  }
  const { leaveType, fromDate, toDate, reason } = req.body;
  if (!leaveType || !fromDate || !toDate || !reason) {
    throw new ApiError(400, 'leaveType, fromDate, toDate, and reason are required');
  }
  if (!LEAVE_TYPES.includes(leaveType)) {
    throw new ApiError(400, `leaveType must be one of: ${LEAVE_TYPES.join(', ')}`);
  }
  const departmentId = resolveTeacherDepartment(req.user);
  if (!departmentId) {
    throw new ApiError(400, 'You are not assigned to a department yet');
  }

  const leave = await TeacherLeaveApplication.create({
    teacherId: req.user._id.toString(),
    departmentId,
    leaveType,
    fromDate,
    toDate,
    reason,
  });
  await leave.populate(POPULATE);
  res.status(201).json({ leave });

  notifyAfterResponse(async () => {
    const [hod, admins] = await Promise.all([resolveHodOfDepartment(departmentId), resolveAllAdmins()]);
    await notifyUsers([...hod, ...admins], {
      type: NotificationType.LEAVE_APPLIED,
      title: `Teacher leave request: ${req.user.fullName}`,
      body: reason,
      actorId: req.user._id.toString(),
      relatedType: 'teacherLeaveApplication',
      relatedId: leave._id.toString(),
    });
  });
});

/** Teacher sees only their own full history; HOD sees their department's queue (optionally
 * filtered to status); Admin sees everything. Unlike Student leave, a Teacher never sees
 * other Teachers' requests - there's no "peer review" concept here, only HOD/Admin. */
const list = asyncHandler(async (req, res) => {
  const { status } = req.query;
  const filter = {};

  if (req.user.role === UserRole.TEACHER) {
    filter.teacherId = req.user._id.toString();
  } else if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
  } else if (req.user.role === UserRole.ADMIN) {
    // unfiltered
  } else {
    throw new ApiError(403, 'Not authorized to view teacher leave applications');
  }

  if (status) filter.status = status;

  const leaves = await TeacherLeaveApplication.find(filter).sort({ createdAt: -1 }).populate(POPULATE);
  res.json({ leaves });
});

/** Only the department's own HOD, or an Admin - deliberately narrower than Student leave's
 * "any Teacher/HOD" rule (see the model's file comment for why). "First responder wins": the
 * findOneAndUpdate's status:'PENDING' filter is the concurrency guard against two HOD sessions
 * deciding the same request at once. */
async function decide(req, res, status) {
  const leave = await TeacherLeaveApplication.findById(req.params.id);
  if (!leave) throw new ApiError(404, 'Leave application not found');

  const authorized = req.user.role === UserRole.ADMIN || isHodOfDepartment(req.user, leave.departmentId);
  if (!authorized) {
    throw new ApiError(403, 'Only the department HOD (or an Admin) can decide this leave application');
  }

  const update = { status, reviewerId: req.user._id.toString(), reviewerRole: req.user.role, decidedAt: new Date() };
  if (status === 'REJECTED') {
    const reason = (req.body.reason || '').trim();
    if (!reason) throw new ApiError(400, 'A reason is required when rejecting a leave application');
    update.rejectionReason = reason;
  }

  const updated = await TeacherLeaveApplication.findOneAndUpdate(
    { _id: leave._id, status: 'PENDING' },
    update,
    { new: true }
  ).populate(POPULATE);

  if (!updated) {
    throw new ApiError(409, 'This leave application has already been decided');
  }
  res.json({ leave: updated });

  notifyAfterResponse(async () => {
    const teacherId = updated.teacherId._id ? updated.teacherId._id.toString() : updated.teacherId.toString();
    await notifyUsers([teacherId], {
      type: status === 'APPROVED' ? NotificationType.LEAVE_APPROVED : NotificationType.LEAVE_REJECTED,
      title: `Leave ${status === 'APPROVED' ? 'approved' : 'rejected'}`,
      body: status === 'REJECTED' ? updated.rejectionReason : updated.reason,
      actorId: req.user._id.toString(),
      relatedType: 'teacherLeaveApplication',
      relatedId: updated._id.toString(),
    });
  });
}

const approve = asyncHandler((req, res) => decide(req, res, 'APPROVED'));
const reject = asyncHandler((req, res) => decide(req, res, 'REJECTED'));

module.exports = { apply, list, approve, reject };
