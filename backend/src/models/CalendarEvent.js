const mongoose = require('mongoose');
const { Schema } = mongoose;

const calendarEventSchema = new Schema(
  {
    title: { type: String, required: true, trim: true },
    description: { type: String, default: '' },
    type: { type: String, enum: ['HOLIDAY', 'EVENT', 'EXAM'], default: 'EVENT' },
    date: { type: Date, required: true },
    createdBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

module.exports = mongoose.model('CalendarEvent', calendarEventSchema);
