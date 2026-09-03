const mongoose = require('mongoose');
const { Schema } = mongoose;

const subjectSchema = new Schema(
  {
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    code: { type: String, required: true, trim: true },
    title: { type: String, required: true, trim: true },
    creditHours: { type: Number, required: true, min: 1 },
    // Unclaimed until a Teacher self-assigns or an HOD/Admin assigns one.
    teacherId: { type: String, ref: 'User', default: null },
  },
  { timestamps: true }
);

subjectSchema.index({ departmentId: 1, code: 1 }, { unique: true });

module.exports = mongoose.model('Subject', subjectSchema);
