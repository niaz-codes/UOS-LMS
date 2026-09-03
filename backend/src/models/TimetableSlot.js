const mongoose = require('mongoose');
const { Schema } = mongoose;

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const timetableSlotSchema = new Schema(
  {
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    teacherId: { type: String, ref: 'User', default: null },
    dayOfWeek: { type: String, enum: DAYS, default: 'MONDAY' },
    startTimeMinutes: { type: Number, required: true },
    endTimeMinutes: { type: Number, required: true },
    room: { type: String, required: true, trim: true },
    roomNormalized: { type: String, required: true },
    createdBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

// Three independent uniqueness constraints, replacing the pre-migration app's three
// deterministic-Firestore-doc-id / index-collection tricks:
timetableSlotSchema.index(
  { departmentId: 1, semesterId: 1, dayOfWeek: 1, startTimeMinutes: 1 },
  { unique: true, name: 'cohort_slot_unique' }
);
timetableSlotSchema.index(
  { teacherId: 1, dayOfWeek: 1, startTimeMinutes: 1 },
  { unique: true, partialFilterExpression: { teacherId: { $type: 'string' } }, name: 'teacher_slot_unique' }
);
timetableSlotSchema.index(
  { roomNormalized: 1, dayOfWeek: 1, startTimeMinutes: 1 },
  { unique: true, name: 'room_slot_unique' }
);

module.exports = mongoose.model('TimetableSlot', timetableSlotSchema);
module.exports.DAYS = DAYS;
