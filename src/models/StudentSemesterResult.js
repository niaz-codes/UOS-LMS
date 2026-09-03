const mongoose = require('mongoose');
const { ResultStatus, PromotionStatus } = require('../constants/enums');

const { Schema } = mongoose;

const subjectResultSnapshotSchema = new Schema(
  {
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    courseCode: { type: String, required: true },
    subjectName: { type: String, required: true },
    creditHours: { type: Number, required: true },
    marks: { type: Number, required: true },
    grade: { type: String, required: true },
    gpa: { type: Number, required: true },
    status: { type: String, required: true },
  },
  { _id: false }
);

// The read-optimized per-student-per-semester rollup (GPA/CGPA/promotion status). Rebuilt
// entirely by services/recalculateSemesterGpa.js - never written to piecemeal elsewhere,
// per spec §5's "single reusable service function" requirement.
const studentSemesterResultSchema = new Schema(
  {
    studentId: { type: String, ref: 'User', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    sessionId: { type: Schema.Types.ObjectId, ref: 'Session', default: null },

    subjectResults: { type: [subjectResultSnapshotSchema], default: [] },
    semesterGPA: { type: Number, default: 0 },
    previousSemesterGPA: { type: Number, default: null },
    cumulativeCGPA: { type: Number, default: 0 },

    resultStatus: { type: String, enum: Object.values(ResultStatus), default: ResultStatus.DRAFT },
    promotionStatus: { type: String, enum: Object.values(PromotionStatus), default: PromotionStatus.NOT_EVALUATED },
    failedSubjectIds: { type: [{ type: Schema.Types.ObjectId, ref: 'Subject' }], default: [] },
  },
  { timestamps: true }
);

studentSemesterResultSchema.index({ studentId: 1, semesterId: 1 }, { unique: true });

module.exports = mongoose.model('StudentSemesterResult', studentSemesterResultSchema);
