const mongoose = require('mongoose');
const { Schema } = mongoose;

const messageSchema = new Schema(
  {
    conversationId: { type: String, ref: 'Conversation', required: true },
    senderId: { type: String, ref: 'User', required: true },
    recipientId: { type: String, ref: 'User', required: true },
    text: { type: String, default: '' },

    mediaId: { type: Schema.Types.ObjectId, ref: 'Media', default: null },
    attachmentUrl: { type: String, default: null },
    attachmentName: { type: String, default: null },
    attachmentPublicId: { type: String, default: null },
    attachmentResourceType: { type: String, default: null },
    attachmentSize: { type: Number, default: null },

    sentAt: { type: Date, default: Date.now },
    // Stamped when the recipient's client fetches this message at all (listMessages) and
    // when they actually open the thread (markRead) - single-tick/double-tick/blue-tick
    // semantics without a real-time channel, since this backend is REST/polling only.
    deliveredAt: { type: Date, default: null },
    readAt: { type: Date, default: null },
  },
  { timestamps: true }
);

messageSchema.index({ conversationId: 1, sentAt: 1 });

module.exports = mongoose.model('Message', messageSchema);
