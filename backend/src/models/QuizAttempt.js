const mongoose = require('mongoose');
const { Schema } = mongoose;

const quizAnswerSchema = new Schema(
  {
    optionIndex: { type: Number, default: null },
    textAnswer: { type: String, default: null },
  },
  { _id: false }
);

const quizAttemptSchema = new Schema(
  {
    quizId: { type: Schema.Types.ObjectId, ref: 'Quiz', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    studentId: { type: String, ref: 'User', required: true },

    // Parallel array to Quiz.questions - answers[i] is this student's answer to
    // questions[i] (optionIndex for MCQ, textAnswer for TEXT). Populated incrementally via
    // saveProgress while the attempt is in progress (see quizController.saveProgress), not
    // just at final submit - this is what lets a student resume after closing the app.
    answers: { type: [quizAnswerSchema], default: [] },

    // Computed server-side at submit time from Quiz.questions[].correctOptionIndex vs
    // answers - never trust a client-supplied score (the pre-migration app computed this
    // on-device, which let a malicious client submit any score; fixed here). Only reflects
    // MCQ questions until any TEXT questions are manually graded.
    score: { type: Number, required: true, default: 0 },
    totalMarks: { type: Number, required: true },

    // Set by startAttempt when the student first opens the quiz - the timer's real anchor,
    // so re-opening the app can't reset the countdown back to full time (see
    // quizController.startAttempt/getRemainingMillis).
    startedAt: { type: Date, default: null },
    // null while in progress; set by submitAttempt once finalized. An attempt therefore has
    // three states: not started (no doc), in progress (startedAt set, submittedAt null),
    // submitted (both set) - the Android UI branches on exactly this.
    submittedAt: { type: Date, default: null },

    // Manual override for TEXT-question marks (or legacy EXAM-type quizzes) - null means
    // the auto-computed score stands.
    manualScore: { type: Number, default: null },
    feedback: { type: String, default: null },
    gradedBy: { type: String, ref: 'User', default: null },
    gradedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

quizAttemptSchema.virtual('effectiveScore').get(function effectiveScore() {
  return this.manualScore !== null && this.manualScore !== undefined ? this.manualScore : this.score;
});
quizAttemptSchema.set('toJSON', { virtuals: true });

// One attempt per student per quiz - immutable after creation (no update path for the
// student; only gradeAttempt touches manualScore/feedback/gradedBy/gradedAt).
quizAttemptSchema.index({ quizId: 1, studentId: 1 }, { unique: true });

module.exports = mongoose.model('QuizAttempt', quizAttemptSchema);
