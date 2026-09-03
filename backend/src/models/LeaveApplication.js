const mongoose = require('mongoose');
const { Schema } = mongoose;

const leaveApplicationSchema = new Schema(
  {
    studentId: { type: String, ref: 'User', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', default: null },

    fromDate: { type: Date, required: true },
    toDate: { type: Date, required: true },
    reason: { type: String, required: true },

    mediaId: { type: Schema.Types.ObjectId, ref: 'Media', default: null },
    attachmentUrl: { type: String, default: null },
    attachmentName: { type: String, default: null },
    attachmentPublicId: { type: String, default: null },
    attachmentResourceType: { type: String, default: null },
    attachmentSize: { type: Number, default: null },

    status: { type: String, enum: ['PENDING', 'APPROVED', 'REJECTED'], default: 'PENDING' },
    reviewerId: { type: String, ref: 'User', default: null },
    reviewerRole: { type: String, default: null },
    decidedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('LeaveApplication', leaveApplicationSchema);
