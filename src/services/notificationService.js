const User = require('../models/User');
const Notification = require('../models/Notification');
const { sendPushToTokens } = require('./fcm');
const { UserRole, UserStatus, NOTIFICATION_TYPE_CATEGORY } = require('../constants/enums');

/** Creates one Notification per recipient (always - the in-app Notification Center is meant to
 * be a complete history regardless of push settings) and best-effort sends a push to whichever
 * of those recipients have push + this notification's category enabled. Never throws - a
 * notification is always secondary to the action that triggered it (e.g. an assignment must
 * still get created even if every downstream notification/push step fails). */
async function notifyUsers(recipientIds, { type, title, body, actorId, relatedType, relatedId }) {
  try {
    const uniqueRecipientIds = [...new Set((recipientIds || []).filter(Boolean))]
      .filter((id) => id !== actorId);
    if (uniqueRecipientIds.length === 0) return [];

    const category = NOTIFICATION_TYPE_CATEGORY[type];
    if (!category) {
      console.error(`notifyUsers: unknown NotificationType "${type}" - skipping`);
      return [];
    }

    const docs = uniqueRecipientIds.map((recipientId) => ({
      recipientId,
      actorId: actorId || null,
      type,
      category,
      title,
      body: body || '',
      relatedType: relatedType || null,
      relatedId: relatedId || null,
    }));
    const created = await Notification.insertMany(docs);

    // Fire-and-forget - push delivery must never block or fail the caller's own request.
    sendPushBestEffort(uniqueRecipientIds, category, { type, title, body, relatedType, relatedId })
      .catch((err) => console.error('notifyUsers: push delivery failed:', err.message));

    return created;
  } catch (err) {
    console.error('notifyUsers failed:', err.message);
    return [];
  }
}

async function sendPushBestEffort(recipientIds, category, payload) {
  const recipients = await User.find({ _id: { $in: recipientIds } })
    .select('fcmTokens notificationSettings')
    .lean();

  await Promise.all(recipients.map(async (recipient) => {
    const settings = recipient.notificationSettings || {};
    const pushEnabled = settings.pushEnabled !== false;
    const categoryEnabled = settings[category] !== false;
    const tokens = recipient.fcmTokens || [];
    if (!pushEnabled || !categoryEnabled || tokens.length === 0) return;

    const { invalidTokens } = await sendPushToTokens(tokens, {
      title: payload.title,
      body: payload.body,
      data: {
        type: payload.type,
        category,
        relatedType: payload.relatedType,
        relatedId: payload.relatedId,
      },
    });
    if (invalidTokens.length > 0) {
      await User.updateOne({ _id: recipient._id }, { $pull: { fcmTokens: { $in: invalidTokens } } });
    }
  }));
}

// --- Recipient-resolution helpers, shared across controllers ---

async function resolveStudentsInCohort(departmentId, semesterId) {
  if (!departmentId || !semesterId) return [];
  const students = await User.find({
    role: UserRole.STUDENT,
    status: UserStatus.APPROVED,
    departmentId,
    currentSemesterId: semesterId,
  }).select('_id').lean();
  return students.map((s) => s._id);
}

async function resolveHodOfDepartment(departmentId) {
  if (!departmentId) return [];
  const hod = await User.findOne({ role: UserRole.HOD, status: UserStatus.APPROVED, departmentId }).select('_id').lean();
  return hod ? [hod._id] : [];
}

/** Teacher is multi-department (departmentIds) - mirrors services/authorization.js's
 * isTeacherInDepartment, which also checks both the legacy scalar and the array field. */
async function resolveTeachersInDepartment(departmentId) {
  if (!departmentId) return [];
  const teachers = await User.find({
    role: UserRole.TEACHER,
    status: UserStatus.APPROVED,
    $or: [{ departmentId }, { departmentIds: departmentId }],
  }).select('_id').lean();
  return teachers.map((t) => t._id);
}

async function resolveAllAdmins() {
  const admins = await User.find({ role: UserRole.ADMIN, status: UserStatus.APPROVED }).select('_id').lean();
  return admins.map((a) => a._id);
}

async function resolveAllApprovedUsers() {
  const users = await User.find({ status: UserStatus.APPROVED }).select('_id').lean();
  return users.map((u) => u._id);
}

/** Runs `task` (typically: resolve recipient ids, then call notifyUsers) without letting any
 * failure propagate to the caller. Controllers call this AFTER already sending their HTTP
 * response, so an uncaught rejection reaching Express's error-handling middleware at that point
 * would crash on "Cannot set headers after they are sent" instead of the notification simply
 * not going out. */
function notifyAfterResponse(task) {
  Promise.resolve()
    .then(task)
    .catch((err) => console.error('notifyAfterResponse failed:', err.message));
}

module.exports = {
  notifyUsers,
  notifyAfterResponse,
  resolveStudentsInCohort,
  resolveHodOfDepartment,
  resolveTeachersInDepartment,
  resolveAllAdmins,
  resolveAllApprovedUsers,
};
