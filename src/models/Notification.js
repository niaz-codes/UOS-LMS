const mongoose = require('mongoose');
const { NotificationType, NotificationCategory } = require('../constants/enums');

const { Schema } = mongoose;

const notificationSchema = new Schema(
  {
    recipientId: { type: String, ref: 'User', required: true },
    // Who/what caused this - null for system-generated notifications with no single actor.
    actorId: { type: String, ref: 'User', default: null },
    type: { type: String, enum: Object.values(NotificationType), required: true },
    category: { type: String, enum: Object.values(NotificationCategory), required: true },
    title: { type: String, required: true },
    body: { type: String, default: '' },
    // Lets the client deep-link ("tap to open correct screen") without parsing the title/body.
    relatedType: { type: String, default: null },
    relatedId: { type: String, default: null },
    read: { type: Boolean, default: false },
    readAt: { type: Date, default: null },
  },
  { timestamps: true }
);

notificationSchema.index({ recipientId: 1, createdAt: -1 });
notificationSchema.index({ recipientId: 1, read: 1 });

module.exports = mongoose.model('Notification', notificationSchema);
