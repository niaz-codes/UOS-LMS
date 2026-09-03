const mongoose = require('mongoose');
const { Schema } = mongoose;

// Separate from LeaveApplication (Student leave) even though the shape overlaps - the
// approval rule is genuinely different (only the department HOD/Admin may decide a Teacher's
// leave, vs. "any Teacher or HOD" for a Student's), and a Teacher's request additionally
// carries a leaveType with no Student-side equivalent. Keeping these as two collections avoids
// a shared `applicantId`-style field that would force identical authorization logic on both.
const teacherLeaveApplicationSchema = new Schema(
  {
    teacherId: { type: String, ref: 'User', required: true },
    // Resolved at apply-time from the applying Teacher's own department(s) - see
    // teacherLeaveController.resolveTeacherDepartment. This is also exactly what scopes a
    // HOD's decide() authority to their own department's teachers.
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },

    leaveType: { type: String, enum: ['CASUAL', 'SICK', 'ANNUAL', 'OTHER'], required: true },
    fromDate: { type: Date, required: true },
    toDate: { type: Date, required: true },
    reason: { type: String, required: true },

    status: { type: String, enum: ['PENDING', 'APPROVED', 'REJECTED'], default: 'PENDING' },
    reviewerId: { type: String, ref: 'User', default: null },
    reviewerRole: { type: String, default: null },
    decidedAt: { type: Date, default: null },
    // Required by the controller whenever status is set to REJECTED - null otherwise.
    rejectionReason: { type: String, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('TeacherLeaveApplication', teacherLeaveApplicationSchema);
