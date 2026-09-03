const mongoose = require('mongoose');
const cloudinary = require('../config/cloudinary');
const User = require('../models/User');
const Subject = require('../models/Subject');
const Media = require('../models/Media');
const ExamResult = require('../models/ExamResult');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const Promotion = require('../models/Promotion');
const AttendanceRecord = require('../models/AttendanceRecord');
const AssignmentSubmission = require('../models/AssignmentSubmission');
const QuizAttempt = require('../models/QuizAttempt');
const TeacherAttendanceRecord = require('../models/TeacherAttendanceRecord');
const LeaveApplication = require('../models/LeaveApplication');
const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const Notification = require('../models/Notification');
const TimetableSlot = require('../models/TimetableSlot');
const ExamSchedule = require('../models/ExamSchedule');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { assignRollNumber } = require('../services/studentNumbering');
const { UserRole, UserStatus, MediaCategory, NotificationType } = require('../constants/enums');
const { isValidMessagingPair } = require('../services/messagingPolicy');
const { notifyUsers, notifyAfterResponse, resolveHodOfDepartment } = require('../services/notificationService');

/** Blocks an Admin from targeting their own account on a destructive/demoting action
 * (delete, suspend, reject, role-change) - the accidental-self-deletion incident this guards
 * against is exactly why it exists. Checked independently of role: even if the caller somehow
 * isn't an Admin, self-targeting a destructive action never makes sense here. */
function assertNotSelf(req, targetUser, message) {
  if (req.user._id.toString() === targetUser._id.toString()) {
    throw new ApiError(403, message);
  }
}

/** Blocks removing/demoting/deactivating the last remaining active Admin account, whoever's
 * making the request. No-ops for a target that isn't currently an active Admin (re-approving a
 * suspended admin, or acting on a non-admin, never reduces the active-admin count). */
async function assertNotLastActiveAdmin(targetUser) {
  if (targetUser.role !== UserRole.ADMIN || targetUser.status !== UserStatus.APPROVED) return;
  const otherActiveAdmins = await User.countDocuments({
    role: UserRole.ADMIN,
    status: UserStatus.APPROVED,
    _id: { $ne: targetUser._id },
  });
  if (otherActiveAdmins === 0) {
    throw new ApiError(403, 'This is the last active Admin account - at least one must remain.');
  }
}

/** A Student's own academic footprint (results, semester rollups, promotions, attendance,
 * submitted work) IS the institutional record - every screen that shows it queries by
 * studentId, so deleting the account would make that data unreachable through any normal path,
 * which is indistinguishable from destroying it. Returned as named counts so a 409 can explain
 * exactly what's blocking the delete; Suspend (see suspend()) is the correct way to remove a
 * student with history from active use without touching this data. */
async function studentAcademicFootprint(userId) {
  const [examResults, semesterResults, promotions, attendanceRecords, assignmentSubmissions, quizAttempts] = await Promise.all([
    ExamResult.countDocuments({ studentId: userId }),
    StudentSemesterResult.countDocuments({ studentId: userId }),
    Promotion.countDocuments({ studentId: userId }),
    AttendanceRecord.countDocuments({ studentId: userId }),
    AssignmentSubmission.countDocuments({ studentId: userId }),
    QuizAttempt.countDocuments({ studentId: userId }),
  ]);
  return { examResults, semesterResults, promotions, attendanceRecords, assignmentSubmissions, quizAttempts };
}

/** A Teacher's OWN attendance (as an employee - distinct from the attendance they marked for
 * students) is their personal institutional record for the same reason. Everything else a
 * teacher authored (grades entered, attendance marked, material uploaded) stays fully
 * meaningful without a live account - see remove() below, which preserves those with the
 * author reference left as-is instead of blocking on them. */
async function teacherAcademicFootprint(userId) {
  const ownAttendanceRecords = await TeacherAttendanceRecord.countDocuments({ teacherId: userId });
  return { ownAttendanceRecords };
}

function describeFootprint(footprint) {
  return Object.entries(footprint)
    .filter(([, count]) => count > 0)
    .map(([key, count]) => `${count} ${key.replace(/([A-Z])/g, ' $1').toLowerCase().trim()}`)
    .join(', ');
}

const list = asyncHandler(async (req, res) => {
  const { role, status, sessionId, currentSemesterId, retakeSubjectId } = req.query;
  // HOD is forced to their own department regardless of what's passed - opening this route to
  // HOD (for dept-scoped reports/monitor screens) must not let them query other departments.
  const departmentId = req.user.role === UserRole.HOD ? req.user.departmentId : req.query.departmentId;
  const filter = {};
  if (role) filter.role = role;
  if (status) filter.status = status;
  if (departmentId) {
    filter.$or = [{ departmentId }, { departmentIds: departmentId }];
  }
  if (sessionId) filter.sessionId = sessionId;
  if (currentSemesterId) filter.currentSemesterId = currentSemesterId;
  if (retakeSubjectId) filter.retakeSubjectIds = retakeSubjectId;

  const users = await User.find(filter).sort({ createdAt: -1 });
  res.json({ users: users.map((u) => u.toPublicJSON()) });
});

const getById = asyncHandler(async (req, res) => {
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  const isSelf = req.user._id.toString() === user._id.toString();
  if (!isSelf && req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Not authorized to view this user');
  }
  res.json({ user: user.toPublicJSON() });
});

const REASON_REQUIRED_STATUSES = [UserStatus.REJECTED, UserStatus.SUSPENDED];

const updateStatus = asyncHandler(async (req, res) => {
  const { status, reason } = req.body;
  if (!Object.values(UserStatus).includes(status)) {
    throw new ApiError(400, `status must be one of: ${Object.values(UserStatus).join(', ')}`);
  }
  if (REASON_REQUIRED_STATUSES.includes(status) && !reason?.trim()) {
    throw new ApiError(400, `a reason is required to set status to ${status}`);
  }

  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  assertNotSelf(req, user, 'You cannot change your own account status.');
  // Only a non-APPROVED target status is "deactivating" - re-approving never reduces the
  // active-admin count, so it's exempt from the last-admin check.
  if (status !== UserStatus.APPROVED) {
    await assertNotLastActiveAdmin(user);
  }

  const previousStatus = user.status;
  user.status = status;
  user.statusHistory.push({ status, at: new Date(), by: req.user._id, reason: reason || null });
  await user.save();

  res.json({ user: user.toPublicJSON() });

  notifyAfterResponse(async () => {
    let type;
    if (status === UserStatus.APPROVED) {
      type = previousStatus === UserStatus.SUSPENDED ? NotificationType.ACCOUNT_REACTIVATED : NotificationType.ACCOUNT_APPROVED;
    } else if (status === UserStatus.REJECTED) {
      type = NotificationType.ACCOUNT_REJECTED;
    } else if (status === UserStatus.SUSPENDED) {
      type = NotificationType.ACCOUNT_SUSPENDED;
    } else {
      return;
    }
    await notifyUsers([user._id.toString()], {
      type,
      title: `Your account is now ${status}`,
      body: reason || '',
      actorId: req.user._id.toString(),
      relatedType: 'user',
      relatedId: user._id.toString(),
    });
  });
});

const updateDepartment = asyncHandler(async (req, res) => {
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  if (user.role === UserRole.TEACHER) {
    const { departmentIds } = req.body;
    if (!Array.isArray(departmentIds)) {
      throw new ApiError(400, 'departmentIds must be an array for TEACHER users');
    }
    user.departmentIds = departmentIds;
  } else {
    // unassign is a plain boolean flag rather than relying on a literal JSON null for
    // departmentId - Gson (and similar clients) omit null fields on serialize by default,
    // which would otherwise silently no-op instead of clearing the assignment.
    const { departmentId, unassign } = req.body;
    if (!departmentId && !unassign) {
      throw new ApiError(400, 'departmentId is required for this user\'s role (or unassign: true)');
    }
    user.departmentId = unassign ? null : departmentId;
  }

  await user.save();

  // A STUDENT's roll number depends on department + session - (re)assign it the moment the
  // department changes, so a placed student never keeps a roll number from another cohort.
  if (user.role === UserRole.STUDENT) {
    await assignRollNumber(user, { placementChanged: true });
  }

  res.json({ user: user.toPublicJSON() });
});

/** Admin places an approved STUDENT into a cohort/semester - there's no other path to set
 * these fields today (registration doesn't collect them). */
const updateAcademicPlacement = asyncHandler(async (req, res) => {
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');
  if (user.role !== UserRole.STUDENT) {
    throw new ApiError(400, 'Academic placement only applies to STUDENT users');
  }

  // clearSession/clearSemester are explicit flags for the same reason unassign is on
  // updateDepartment - a client-omitted null would otherwise be indistinguishable from
  // "leave this field alone".
  const previousSessionId = user.sessionId ? user.sessionId.toString() : null;
  const { sessionId, currentSemesterId, clearSession, clearSemester } = req.body;
  if (clearSession) user.sessionId = null;
  else if (sessionId !== undefined) user.sessionId = sessionId;
  if (clearSemester) user.currentSemesterId = null;
  else if (currentSemesterId !== undefined) user.currentSemesterId = currentSemesterId;
  await user.save();

  // Assigning a session completes the department+session pair, which is exactly when the
  // roll number can be generated (see studentNumbering.assignRollNumber).
  const sessionChanged = previousSessionId !== (user.sessionId ? user.sessionId.toString() : null);
  await assignRollNumber(user, { placementChanged: sessionChanged });

  res.json({ user: user.toPublicJSON() });
});

/** Self or Admin. */
const updateProfile = asyncHandler(async (req, res) => {
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  const isSelf = req.user._id.toString() === user._id.toString();
  if (!isSelf && req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Not authorized to edit this user');
  }

  const { fullName, fatherName, phone, cnic } = req.body;
  if (fullName !== undefined) user.fullName = fullName;
  if (fatherName !== undefined) user.fatherName = fatherName;
  if (phone !== undefined) user.phone = phone;
  if (cnic !== undefined) user.cnic = cnic;

  try {
    await user.save();
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, 'That CNIC or phone number is already registered to another account');
    throw err;
  }
  res.json({ user: user.toPublicJSON() });
});

/** Admin only - role-specific identifiers (employeeId/designation for HOD/TEACHER,
 * registrationNumber/rollNumber for STUDENT). */
const updateIdentifiers = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can edit identifiers');
  }
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  const { employeeId, designation, registrationNumber, rollNumber } = req.body;
  if (employeeId !== undefined) user.employeeId = employeeId;
  if (designation !== undefined) user.designation = designation;
  if (registrationNumber !== undefined) user.registrationNumber = registrationNumber;
  if (rollNumber !== undefined) user.rollNumber = rollNumber;

  try {
    await user.save();
  } catch (err) {
    // The sparse unique indexes added for auto-numbering also guard manual edits.
    if (err.code === 11000) {
      throw new ApiError(409, 'That registration number or roll number is already assigned to another student');
    }
    throw err;
  }
  res.json({ user: user.toPublicJSON() });
});

/** Flexible count aggregate covering every count the Admin User Management tree needs:
 * total by role, by role within a department, students within a session/semester, etc. -
 * one endpoint instead of half a dozen single-purpose ones. */
const counts = asyncHandler(async (req, res) => {
  const { role, sessionId, semesterId } = req.query;
  const departmentId = req.user.role === UserRole.HOD ? req.user.departmentId : req.query.departmentId;
  const filter = {};
  if (role) filter.role = role;
  if (departmentId) filter.$or = [{ departmentId }, { departmentIds: departmentId }];
  if (sessionId) filter.sessionId = sessionId;
  if (semesterId) filter.currentSemesterId = semesterId;

  const count = await User.countDocuments(filter);
  res.json({ count });
});

/** The one existing APPROVED HOD of a department, if any - used to warn the Admin before
 * reassigning a department to a different HOD (they'd be silently displacing someone). */
const hodForDepartment = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can look this up');
  }
  const hod = await User.findOne({ role: UserRole.HOD, departmentId: req.params.departmentId, status: UserStatus.APPROVED });
  res.json({ user: hod ? hod.toPublicJSON() : null });
});

const updateRole = asyncHandler(async (req, res) => {
  const { role } = req.body;
  if (!Object.values(UserRole).includes(role)) {
    throw new ApiError(400, `role must be one of: ${Object.values(UserRole).join(', ')}`);
  }
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  assertNotSelf(req, user, 'You cannot change your own role.');
  if (user.role === UserRole.ADMIN && role !== UserRole.ADMIN) {
    await assertNotLastActiveAdmin(user);
  }

  user.role = role;
  await user.save();
  res.json({ user: user.toPublicJSON() });

  notifyAfterResponse(async () => {
    await notifyUsers([user._id.toString()], {
      type: NotificationType.ACCOUNT_ROLE_CHANGED,
      title: `Your role is now ${role}`,
      body: '',
      actorId: req.user._id.toString(),
      relatedType: 'user',
      relatedId: user._id.toString(),
    });
  });
});

/** Permanently deletes a user from MongoDB Atlas - not a status flip. Every related collection
 * is inspected first (see studentAcademicFootprint/teacherAcademicFootprint) and handled per
 * the same rule for every role: a record that only has meaning attached to THIS user's own
 * identity blocks the delete (409, named counts, "Suspend instead"); a record this user merely
 * authored/graded/marked for someone else stays fully meaningful without them and is preserved
 * as-is; a currently-active assignment pointer (Subject.teacherId, TimetableSlot.teacherId,
 * ExamSchedule.teacherId/invigilatorId) is freed back to unassigned rather than left dangling;
 * purely personal data (conversations, messages, notifications, the profile photo) has no
 * independent value and is cascade-deleted. All of it runs in one transaction so a mid-sequence
 * failure can't leave the account half-deleted. */
const remove = asyncHandler(async (req, res) => {
  const user = await User.findById(req.params.id);
  if (!user) throw new ApiError(404, 'User not found');

  assertNotSelf(req, user, 'You cannot delete your own account.');
  await assertNotLastActiveAdmin(user);

  if (user.role === UserRole.STUDENT) {
    const footprint = await studentAcademicFootprint(user._id);
    if (Object.values(footprint).some((count) => count > 0)) {
      throw new ApiError(409, `This student has existing academic records (${describeFootprint(footprint)}) and cannot be permanently deleted. Suspend the account instead to remove access while preserving institutional history.`);
    }
  } else if (user.role === UserRole.TEACHER) {
    const footprint = await teacherAcademicFootprint(user._id);
    if (footprint.ownAttendanceRecords > 0) {
      throw new ApiError(409, `This teacher has an existing attendance history (${describeFootprint(footprint)}) and cannot be permanently deleted. Suspend the account instead to remove access while preserving institutional history.`);
    }
  }

  const userId = user._id.toString();
  const profilePhotoPublicId = user.profilePhotoPublicId;
  let messageAttachments = [];

  const session = await mongoose.startSession();
  try {
    await session.withTransaction(async () => {
      if (user.role === UserRole.TEACHER) {
        await Subject.updateMany({ teacherId: userId }, { teacherId: null }, { session });
        await TimetableSlot.updateMany({ teacherId: userId }, { teacherId: null }, { session });
        await ExamSchedule.updateMany({ teacherId: userId }, { teacherId: null }, { session });
        await ExamSchedule.updateMany({ invigilatorId: userId }, { invigilatorId: null }, { session });
      }
      if (user.role === UserRole.STUDENT) {
        await LeaveApplication.deleteMany({ studentId: userId }, { session });
      }

      const conversations = await Conversation.find(
        { $or: [{ participantAId: userId }, { participantBId: userId }] }
      ).session(session);
      const conversationIds = conversations.map((c) => c._id);
      if (conversationIds.length > 0) {
        const messages = await Message.find({ conversationId: { $in: conversationIds } })
          .select('attachmentPublicId attachmentResourceType')
          .session(session);
        messageAttachments = messages
          .filter((m) => m.attachmentPublicId)
          .map((m) => ({ publicId: m.attachmentPublicId, resourceType: m.attachmentResourceType || 'image' }));
        await Message.deleteMany({ conversationId: { $in: conversationIds } }, { session });
        await Conversation.deleteMany({ _id: { $in: conversationIds } }, { session });
      }

      await Notification.deleteMany({ recipientId: userId }, { session });
      await Media.deleteOne({ category: MediaCategory.PROFILE_PHOTO, uploadedBy: userId }, { session });

      await user.deleteOne({ session });
    });
  } finally {
    await session.endSession();
  }

  // Cloudinary cleanup is best-effort and happens after the transaction commits - the account
  // is already correctly gone from MongoDB either way, and a Cloudinary hiccup here must not
  // be reported back as the whole delete having failed.
  const cloudinaryCleanup = [];
  if (profilePhotoPublicId) {
    cloudinaryCleanup.push(cloudinary.uploader.destroy(profilePhotoPublicId));
  }
  for (const attachment of messageAttachments) {
    cloudinaryCleanup.push(cloudinary.uploader.destroy(attachment.publicId, { resource_type: attachment.resourceType }));
  }
  await Promise.allSettled(cloudinaryCleanup);

  res.status(204).send();
});

/** Self-service, Teacher only: joining a department needs no approval (mirrors the
 * pre-migration Firestore rules) - it's what unlocks self-assigning that department's
 * unclaimed subjects (see subjectController.selfAssign). */
const joinDepartment = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.TEACHER) {
    throw new ApiError(403, 'Only teachers can self-assign a department');
  }
  const { departmentId } = req.params;
  const alreadyJoined = req.user.departmentIds.some((id) => id.toString() === departmentId);
  if (!alreadyJoined) {
    req.user.departmentIds.push(departmentId);
    await req.user.save();
  }
  res.json({ user: req.user.toPublicJSON() });

  if (!alreadyJoined) {
    notifyAfterResponse(async () => {
      const hodIds = await resolveHodOfDepartment(departmentId);
      await notifyUsers(hodIds, {
        type: NotificationType.ACCOUNT_DEPARTMENT_ASSIGNED,
        title: `${req.user.fullName} joined your department`,
        body: '',
        actorId: req.user._id.toString(),
        relatedType: 'user',
        relatedId: req.user._id.toString(),
      });
    });
  }
});

/** Self-scoped: every APPROVED user this caller is allowed to start a conversation with,
 * per the app-wide messaging role-pair policy (Student<->Teacher, Teacher<->HOD, HOD<->Admin,
 * same department). Any approved role may call this for themselves - unlike list()/counts(),
 * there's no broader roster being exposed, just this one policy filter. */
const contacts = asyncHandler(async (req, res) => {
  const candidates = await User.find({ status: UserStatus.APPROVED, _id: { $ne: req.user._id } });
  const valid = candidates.filter((candidate) => isValidMessagingPair(req.user, candidate));
  res.json({ users: valid.map((u) => u.toPublicJSON()) });
});

module.exports = {
  list,
  getById,
  updateStatus,
  updateDepartment,
  updateAcademicPlacement,
  updateProfile,
  updateIdentifiers,
  counts,
  hodForDepartment,
  updateRole,
  remove,
  joinDepartment,
  contacts,
};
