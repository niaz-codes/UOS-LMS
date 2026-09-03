const mongoose = require('mongoose');
const { RepeatStatus } = require('../constants/enums');

const { Schema } = mongoose;

// Extends a failed ExamResult rather than replacing it - previousMarks/Grade/Gpa keep the
// original attempt's history even after the repeat is approved and the ExamResult is
// updated in place (see repeatExamController.approve).
const repeatExamSchema = new Schema(
  {
    examResultId: { type: Schema.Types.ObjectId, ref: 'ExamResult', required: true },
    studentId: { type: String, ref: 'User', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    // Denormalized from the originating ExamResult at creation time, purely so the list
    // endpoint can scope an HOD to their own department without an extra join per row.
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },

    previousMarks: { type: Number, required: true },
    previousGrade: { type: String, required: true },
    previousGpa: { type: Number, required: true },

    newMarks: { type: Number, default: null },
    newGrade: { type: String, default: null },
    newGpa: { type: Number, default: null },

    repeatStatus: { type: String, enum: Object.values(RepeatStatus), default: RepeatStatus.PENDING },
    rejectionReason: { type: String, default: null },

    submittedBy: { type: String, ref: 'User', default: null },
    reviewedBy: { type: String, ref: 'User', default: null },
    reviewedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('RepeatExam', repeatExamSchema);
