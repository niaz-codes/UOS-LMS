const mongoose = require('mongoose');
const { Schema } = mongoose;

const assignmentSubmissionSchema = new Schema(
  {
    assignmentId: { type: Schema.Types.ObjectId, ref: 'Assignment', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    studentId: { type: String, ref: 'User', required: true },

    textAnswer: { type: String, default: null },
    mediaId: { type: Schema.Types.ObjectId, ref: 'Media', default: null },
    fileUrl: { type: String, default: null },
    fileName: { type: String, default: null },
    filePublicId: { type: String, default: null },
    fileResourceType: { type: String, default: null },
    fileSize: { type: Number, default: null },

    submittedAt: { type: Date, default: Date.now },

    // null marksObtained == not yet graded (the "isGraded" flag, same as the pre-migration app).
    marksObtained: { type: Number, default: null },
    feedback: { type: String, default: null },
    gradedBy: { type: String, ref: 'User', default: null },
    gradedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

// One submission per student per assignment - resubmitting overwrites (upsert), matching
// the old deterministic "assignmentId_studentUid" doc id.
assignmentSubmissionSchema.index({ assignmentId: 1, studentId: 1 }, { unique: true });

module.exports = mongoose.model('AssignmentSubmission', assignmentSubmissionSchema);
