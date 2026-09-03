const Conversation = require('../models/Conversation');
const Message = require('../models/Message');
const Media = require('../models/Media');
const User = require('../models/User');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isValidMessagingPair } = require('../services/messagingPolicy');
const { notifyUsers, notifyAfterResponse } = require('../services/notificationService');

function conversationId(uidA, uidB) {
  return uidA <= uidB ? `${uidA}_${uidB}` : `${uidB}_${uidA}`;
}

const findOrCreate = asyncHandler(async (req, res) => {
  const { otherUserId } = req.body;
  if (!otherUserId) throw new ApiError(400, 'otherUserId is required');
  if (otherUserId === req.user._id.toString()) throw new ApiError(400, 'Cannot message yourself');

  const other = await User.findById(otherUserId);
  if (!other || other.status !== 'APPROVED') throw new ApiError(404, 'User not found');
  if (!isValidMessagingPair(req.user, other)) {
    throw new ApiError(403, 'You are not allowed to message this user');
  }

  const id = conversationId(req.user._id.toString(), otherUserId);
  const isSelfA = req.user._id.toString() <= otherUserId;

  const conversation = await Conversation.findOneAndUpdate(
    { _id: id },
    {
      $setOnInsert: {
        _id: id,
        participantAId: isSelfA ? req.user._id.toString() : otherUserId,
        participantBId: isSelfA ? otherUserId : req.user._id.toString(),
      },
    },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  ).populate([
    { path: 'participantAId', select: 'fullName role' },
    { path: 'participantBId', select: 'fullName role' },
  ]);

  res.json({ conversation });
});

const listConversations = asyncHandler(async (req, res) => {
  const uid = req.user._id.toString();
  const conversations = await Conversation.find({ $or: [{ participantAId: uid }, { participantBId: uid }] })
    .sort({ lastMessageAt: -1, createdAt: -1 })
    .populate([
      { path: 'participantAId', select: 'fullName role' },
      { path: 'participantBId', select: 'fullName role' },
    ]);
  res.json({ conversations });
});

async function requireParticipant(req, conversationId) {
  const conversation = await Conversation.findById(conversationId);
  if (!conversation) throw new ApiError(404, 'Conversation not found');
  const uid = req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && conversation.participantAId !== uid && conversation.participantBId !== uid) {
    throw new ApiError(403, 'Not a participant in this conversation');
  }
  return conversation;
}

const sendMessage = asyncHandler(async (req, res) => {
  const conversation = await requireParticipant(req, req.params.id);
  const { text, mediaId } = req.body;
  if ((!text || !text.trim()) && !mediaId) {
    throw new ApiError(400, 'text or mediaId is required');
  }

  const senderId = req.user._id.toString();
  const recipientId = conversation.participantAId === senderId ? conversation.participantBId : conversation.participantAId;

  const fields = { conversationId: conversation._id, senderId, recipientId, text: text || '' };
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

  const message = await Message.create(fields);

  const unreadField = conversation.participantAId === recipientId ? 'unreadCountA' : 'unreadCountB';
  conversation.lastMessageText = text || (fields.attachmentName ? `Attachment: ${fields.attachmentName}` : '');
  conversation.lastMessageSenderId = senderId;
  conversation.lastMessageAt = message.sentAt;
  conversation[unreadField] += 1;
  await conversation.save();

  res.status(201).json({ message });

  notifyAfterResponse(() => notifyUsers([recipientId], {
    type: NotificationType.NEW_MESSAGE,
    title: `New message from ${req.user.fullName}`,
    body: message.text || (fields.attachmentName ? `Attachment: ${fields.attachmentName}` : ''),
    actorId: senderId,
    relatedType: 'conversation',
    relatedId: conversation._id.toString(),
  }));
});

/** Optional `since` (ISO timestamp) turns this into a poll-for-new-messages endpoint - the
 * thread screen calls it every few seconds while open instead of a Firestore listener.
 * Also stamps deliveredAt on every message addressed to the caller that hasn't been fetched
 * yet - "delivered" here means "the recipient's client has fetched it at all", the closest
 * approximation available without a real-time push/ack channel. */
const listMessages = asyncHandler(async (req, res) => {
  const conversation = await requireParticipant(req, req.params.id);
  const uid = req.user._id.toString();

  await Message.updateMany(
    { conversationId: conversation._id, recipientId: uid, deliveredAt: null },
    { deliveredAt: new Date() }
  );

  const filter = { conversationId: conversation._id };
  if (req.query.since) filter.sentAt = { $gt: new Date(req.query.since) };

  const messages = await Message.find(filter).sort({ sentAt: 1 });
  res.json({ messages });
});

const markRead = asyncHandler(async (req, res) => {
  const conversation = await requireParticipant(req, req.params.id);
  const uid = req.user._id.toString();
  if (conversation.participantAId === uid) conversation.unreadCountA = 0;
  else if (conversation.participantBId === uid) conversation.unreadCountB = 0;
  await conversation.save();

  const now = new Date();
  // Two separate updates rather than one $set with both fields, so a message that already
  // has an (earlier, real) deliveredAt from a prior listMessages poll keeps that timestamp
  // instead of being overwritten with "now".
  await Message.updateMany(
    { conversationId: conversation._id, recipientId: uid, deliveredAt: null },
    { deliveredAt: now }
  );
  await Message.updateMany(
    { conversationId: conversation._id, recipientId: uid, readAt: null },
    { readAt: now }
  );

  await conversation.populate([
    { path: 'participantAId', select: 'fullName role' },
    { path: 'participantBId', select: 'fullName role' },
  ]);
  res.json({ conversation });
});

/** Every message ever addressed to the caller - the source AppNotificationCenter polls for
 * "new message" notifications (see notifications feed). */
const listMyMessages = asyncHandler(async (req, res) => {
  const filter = { recipientId: req.user._id.toString() };
  if (req.query.since) filter.sentAt = { $gt: new Date(req.query.since) };
  const messages = await Message.find(filter).sort({ sentAt: -1 }).limit(50)
    .populate({ path: 'senderId', select: 'fullName' });
  res.json({ messages });
});

module.exports = { findOrCreate, listConversations, sendMessage, listMessages, markRead, listMyMessages };
