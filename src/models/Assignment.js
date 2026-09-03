const mongoose = require('mongoose');
const { Schema } = mongoose;

const assignmentSchema = new Schema(
  {
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    title: { type: String, required: true, trim: true },
    description: { type: String, default: '' },
    dueDate: { type: Date, required: true },
    maxMarks: { type: Number, required: true, min: 1 },
    createdBy: { type: String, ref: 'User', required: true },
    // Optional Cloudinary attachment via the Media API - null if the teacher didn't attach one.
    // mediaId lets delete cleanly cascade to DELETE /api/media/:id; the rest are denormalized
    // display fields so list screens don't need a join per row.
    mediaId: { type: Schema.Types.ObjectId, ref: 'Media', default: null },
    fileUrl: { type: String, default: null },
    fileName: { type: String, default: null },
    filePublicId: { type: String, default: null },
    fileResourceType: { type: String, default: null },
    fileSize: { type: Number, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Assignment', assignmentSchema);
