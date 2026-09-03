const mongoose = require('mongoose');
const { Schema } = mongoose;

// Curriculum semester (e.g. "Semester 3" of a department), shared across every Session
// (cohort) of that department - deliberately has no sessionId/studentId. Per-student
// results/GPA live in StudentSemesterResult instead (Phase 4), keeping curriculum data
// from being duplicated per student or per cohort.
const semesterSchema = new Schema(
  {
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    number: { type: Number, required: true, min: 1 },
  },
  { timestamps: true }
);

semesterSchema.index({ departmentId: 1, number: 1 }, { unique: true });

module.exports = mongoose.model('Semester', semesterSchema);
