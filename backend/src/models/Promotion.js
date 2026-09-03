const mongoose = require('mongoose');
const { Schema } = mongoose;

// Immutable audit record of a promotion event - "already promoted from this semester?" is
// answered by the unique index below rather than a client-composed deterministic doc id
// (the trick the pre-migration Firestore version relied on).
const promotionSchema = new Schema(
  {
    studentId: { type: String, ref: 'User', required: true },
    fromSemesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    toSemesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    promotedBy: { type: String, ref: 'User', required: true },
    failedSubjectIds: { type: [{ type: Schema.Types.ObjectId, ref: 'Subject' }], default: [] },
  },
  { timestamps: true }
);

promotionSchema.index({ studentId: 1, fromSemesterId: 1 }, { unique: true });

module.exports = mongoose.model('Promotion', promotionSchema);
