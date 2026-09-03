const mongoose = require('mongoose');
const { Schema } = mongoose;

// Course-wise teacher attendance: was this subject's assigned teacher present for THIS course
// on a given day - a teacher can be PRESENT for one course and ABSENT for another on the same
// date, since each record is scoped to one (subject, dateKey) session, not the whole day.
const teacherAttendanceRecordSchema = new Schema(
  {
    teacherId: { type: String, ref: 'User', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    dateKey: { type: String, required: true },
    status: { type: String, enum: ['PRESENT', 'ABSENT'], required: true },
    markedBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

// One record per course per day - upserted, mirrors AttendanceRecord's idempotency pattern.
// Scoping the unique key to (subjectId, dateKey) rather than (teacherId, dateKey) is what lets
// the same teacher have independent PRESENT/ABSENT statuses across different courses same day.
teacherAttendanceRecordSchema.index({ subjectId: 1, dateKey: 1 }, { unique: true });

module.exports = mongoose.model('TeacherAttendanceRecord', teacherAttendanceRecordSchema);
