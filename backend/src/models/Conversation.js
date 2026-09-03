const mongoose = require('mongoose');
const { Schema } = mongoose;

const conversationSchema = new Schema(
  {
    // Deterministic sorted-pair id (same idempotency idiom as the pre-migration app's
    // Firestore doc id) - "find or create" stays a plain upsert, no transaction needed.
    _id: { type: String, required: true },
    participantAId: { type: String, ref: 'User', required: true },
    participantBId: { type: String, ref: 'User', required: true },
    lastMessageText: { type: String, default: null },
    lastMessageSenderId: { type: String, ref: 'User', default: null },
    lastMessageAt: { type: Date, default: null },
    unreadCountA: { type: Number, default: 0 },
    unreadCountB: { type: Number, default: 0 },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Conversation', conversationSchema);
