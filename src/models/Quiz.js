const mongoose = require('mongoose');
const { Schema } = mongoose;

const quizQuestionSchema = new Schema(
  {
    text: { type: String, required: true },
    // MCQ = auto-graded from options/correctOptionIndex; TEXT = written answer, always
    // manually graded (see quizController.gradeAttempt) since there's no answer key to
    // auto-score against. options/correctOptionIndex are only meaningful for MCQ - left
    // not-required at the schema level, validated conditionally in the controller.
    questionType: { type: String, enum: ['MCQ', 'TEXT'], default: 'MCQ' },
    options: { type: [String], default: [] },
    correctOptionIndex: { type: Number, default: null },
    marks: { type: Number, required: true, min: 1 },
  },
  { _id: false }
);

const quizSchema = new Schema(
  {
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    title: { type: String, required: true, trim: true },
    description: { type: String, default: '' },
    // Quiz and Exam are the same document shape - only this field distinguishes them (unified
    // entity, matches the pre-migration app; Exam additionally allows manual-grade override).
    type: { type: String, enum: ['QUIZ', 'EXAM'], default: 'QUIZ' },
    questions: { type: [quizQuestionSchema], required: true },
    timeLimitMinutes: { type: Number, required: true, min: 1 },
    dueDate: { type: Date, required: true },
    createdBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

quizSchema.virtual('totalMarks').get(function totalMarks() {
  return this.questions.reduce((sum, q) => sum + q.marks, 0);
});
quizSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Quiz', quizSchema);
