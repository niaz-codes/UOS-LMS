const mongoose = require('mongoose');
const { Schema } = mongoose;

const announcementSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    body: { type: String, required: true },
    authorId: { type: String, ref: 'User', required: true },
    scope: { type: String, enum: ['ALL', 'DEPARTMENT', 'SUBJECT'], default: 'ALL' },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', default: null },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Announcement', announcementSchema);
