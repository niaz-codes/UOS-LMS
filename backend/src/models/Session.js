const mongoose = require('mongoose');
const { Schema } = mongoose;

// A cohort/batch label (e.g. "2022-2026") scoping which students belong to which intake -
// NOT an owner of Semester documents (see Semester.js for why: semesters/subjects are a
// department's shared curriculum, reused across every session of that department).
const sessionSchema = new Schema(
  {
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    label: { type: String, required: true, trim: true },
    isActive: { type: Boolean, default: true },
  },
  { timestamps: true }
);

sessionSchema.index({ departmentId: 1, label: 1 }, { unique: true });

module.exports = mongoose.model('Session', sessionSchema);
