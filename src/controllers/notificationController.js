const Notification = require('../models/Notification');
const User = require('../models/User');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { NotificationCategory } = require('../constants/enums');

const DEFAULT_LIMIT = 30;
const MAX_LIMIT = 100;

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Paginated, filterable, searchable history for the Notification Center. Every notification
 * ever created for this user, regardless of their push settings - settings only gate whether a
 * push was sent, never whether the in-app record exists. */
const list = asyncHandler(async (req, res) => {
  const recipientId = req.user._id.toString();
  const { category, type, read, q } = req.query;
  const limit = Math.min(parseInt(req.query.limit, 10) || DEFAULT_LIMIT, MAX_LIMIT);
  const page = Math.max(parseInt(req.query.page, 10) || 1, 1);

  const filter = { recipientId };
  if (category) filter.category = category;
  if (type) filter.type = type;
  if (read === 'true') filter.read = true;
  else if (read === 'false') filter.read = false;
  if (q) {
    const regex = new RegExp(escapeRegex(q), 'i');
    filter.$or = [{ title: regex }, { body: regex }];
  }

  const [notifications, total, unreadCount] = await Promise.all([
    Notification.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit),
    Notification.countDocuments(filter),
    Notification.countDocuments({ recipientId, read: false }),
  ]);

  res.json({ notifications, total, page, limit, unreadCount });
});

const unreadCount = asyncHandler(async (req, res) => {
  const count = await Notification.countDocuments({ recipientId: req.user._id.toString(), read: false });
  res.json({ unreadCount: count });
});

const markRead = asyncHandler(async (req, res) => {
  const notification = await Notification.findOneAndUpdate(
    { _id: req.params.id, recipientId: req.user._id.toString() },
    { read: true, readAt: new Date() },
    { new: true }
  );
  if (!notification) throw new ApiError(404, 'Notification not found');
  res.json({ notification });
});

const markAllRead = asyncHandler(async (req, res) => {
  await Notification.updateMany(
    { recipientId: req.user._id.toString(), read: false },
    { read: true, readAt: new Date() }
  );
  res.status(204).send();
});

const remove = asyncHandler(async (req, res) => {
  const deleted = await Notification.findOneAndDelete({ _id: req.params.id, recipientId: req.user._id.toString() });
  if (!deleted) throw new ApiError(404, 'Notification not found');
  res.status(204).send();
});

const getSettings = asyncHandler(async (req, res) => {
  res.json({ settings: req.user.notificationSettings });
});

/** Any subset of the boolean toggles may be sent - only recognized, boolean-valued keys are
 * applied, everything else in the body is silently ignored (matches this codebase's existing
 * "PATCH a few fields" convention, e.g. authController.updateMyPhoto). */
const updateSettings = asyncHandler(async (req, res) => {
  const allowedKeys = ['pushEnabled', 'soundEnabled', 'vibrationEnabled', ...Object.values(NotificationCategory)];
  const updates = {};
  for (const key of allowedKeys) {
    if (typeof req.body[key] === 'boolean') {
      updates[`notificationSettings.${key}`] = req.body[key];
    }
  }
  const user = await User.findByIdAndUpdate(req.user._id, { $set: updates }, { new: true });
  res.json({ settings: user.notificationSettings });
});

const registerFcmToken = asyncHandler(async (req, res) => {
  await User.updateOne({ _id: req.user._id }, { $addToSet: { fcmTokens: req.body.token } });
  res.status(204).send();
});

/** Best-effort - called on logout so a signed-out device stops receiving this account's
 * pushes; a token that's already gone (already pruned, or never registered) isn't an error. */
const unregisterFcmToken = asyncHandler(async (req, res) => {
  await User.updateOne({ _id: req.user._id }, { $pull: { fcmTokens: req.body.token } });
  res.status(204).send();
});

module.exports = {
  list,
  unreadCount,
  markRead,
  markAllRead,
  remove,
  getSettings,
  updateSettings,
  registerFcmToken,
  unregisterFcmToken,
};
