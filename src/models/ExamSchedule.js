const mongoose = require('mongoose');
const { Schema } = mongoose;

const examScheduleSchema = new Schema(
  {
    examType: { type: String, enum: ['MID_TERM', 'FINAL_TERM'], default: 'MID_TERM' },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    teacherId: { type: String, ref: 'User', default: null },
    invigilatorId: { type: String, ref: 'User', default: null },
    room: { type: String, required: true, trim: true },
    roomNormalized: { type: String, required: true },
    examDate: { type: Date, required: true },
    examDateKey: { type: String, required: true },
    startTimeMinutes: { type: Number, required: true },
    endTimeMinutes: { type: Number, required: true },
    status: { type: String, enum: ['DRAFT', 'PUBLISHED', 'LOCKED'], default: 'DRAFT' },
    createdBy: { type: String, ref: 'User', required: true },
    publishedAt: { type: Date, default: null },
    lockedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

// No cohort-uniqueness constraint - a department legitimately has many exams on the same
// day for different subjects (matches the pre-migration app). Only invigilator/room clashes
// are guarded.
examScheduleSchema.index(
  { invigilatorId: 1, examDateKey: 1, startTimeMinutes: 1 },
  { unique: true, partialFilterExpression: { invigilatorId: { $type: 'string' } }, name: 'invigilator_slot_unique' }
);
examScheduleSchema.index(
  { roomNormalized: 1, examDateKey: 1, startTimeMinutes: 1 },
  { unique: true, name: 'room_slot_unique' }
);

module.exports = mongoose.model('ExamSchedule', examScheduleSchema);
