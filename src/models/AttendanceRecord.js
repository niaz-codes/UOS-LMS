const mongoose = require('mongoose');
const { Schema } = mongoose;

const attendanceRecordSchema = new Schema(
  {
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    // "yyyy-MM-dd" - the session key, matches the pre-migration app's DateKeyUtils convention.
    dateKey: { type: String, required: true },
    studentId: { type: String, ref: 'User', required: true },
    status: { type: String, enum: ['PRESENT', 'ABSENT'], required: true },
    markedBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

// One record per student per subject per session day - upserted, replacing the old
// deterministic "subjectId_dateKey_studentUid" Firestore doc id.
attendanceRecordSchema.index({ subjectId: 1, dateKey: 1, studentId: 1 }, { unique: true });

module.exports = mongoose.model('AttendanceRecord', attendanceRecordSchema);
