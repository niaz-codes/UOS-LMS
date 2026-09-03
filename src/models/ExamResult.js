const mongoose = require('mongoose');
const { ResultStatus, SubjectResultStatus } = require('../constants/enums');

const { Schema } = mongoose;

const auditEntrySchema = new Schema(
  {
    action: { type: String, required: true },
    by: { type: String, ref: 'User', required: true },
    at: { type: Date, default: Date.now },
    previousValues: { type: Schema.Types.Mixed, default: null },
    newValues: { type: Schema.Types.Mixed, default: null },
  },
  { _id: false }
);

// One document per (studentId, subjectId) - the unit a Teacher submits marks for and an HOD
// approves/rejects. StudentSemesterResult (a separate collection) is the read-optimized
// per-semester rollup, rebuilt from these whenever one of them changes approval state.
const examResultSchema = new Schema(
  {
    studentId: { type: String, ref: 'User', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },

    marks: { type: Number, required: true, min: 0, max: 100 },
    grade: { type: String, required: true },
    gpa: { type: Number, required: true },
    status: { type: String, enum: Object.values(SubjectResultStatus), required: true },

    resultStatus: { type: String, enum: Object.values(ResultStatus), default: ResultStatus.DRAFT },
    rejectionReason: { type: String, default: null },

    // Whether a RepeatExam has been scheduled/submitted/approved for this failed subject
    // is tracked on the RepeatExam document itself (queried by examResultId) rather than
    // duplicated here - this flag only marks that the subject qualifies for one.
    repeatEligible: { type: Boolean, default: false },

    submittedBy: { type: String, ref: 'User', required: true },
    reviewedBy: { type: String, ref: 'User', default: null },
    reviewedAt: { type: Date, default: null },

    auditLog: { type: [auditEntrySchema], default: [] },
  },
  { timestamps: true }
);

examResultSchema.index({ studentId: 1, subjectId: 1 }, { unique: true });

module.exports = mongoose.model('ExamResult', examResultSchema);
