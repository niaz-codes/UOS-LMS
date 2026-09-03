const mongoose = require('mongoose');
const { UserRole, UserStatus, NotificationCategory } = require('../constants/enums');

const { Schema } = mongoose;

// Every category defaults to enabled - matches NotificationPreferenceManager's previous
// single "on" default on the Android side, just split per-category now. Mongoose applies
// each field's own default when the sub-document is first created, so no explicit factory
// function is needed here.
const notificationSettingsSchema = new Schema(
  Object.fromEntries([
    ['pushEnabled', { type: Boolean, default: true }],
    ['soundEnabled', { type: Boolean, default: true }],
    ['vibrationEnabled', { type: Boolean, default: true }],
    ...Object.values(NotificationCategory).map((category) => [category, { type: Boolean, default: true }]),
  ]),
  { _id: false }
);

const statusHistoryEntrySchema = new Schema(
  {
    status: { type: String, enum: Object.values(UserStatus), required: true },
    at: { type: Date, default: Date.now },
    by: { type: String, ref: 'User', default: null },
    reason: { type: String, default: null },
  },
  { _id: false }
);

const userSchema = new Schema(
  {
    // Deliberately a String, not an auto ObjectId - a holdover from the Firebase migration
    // period when this had to equal the Firebase Auth uid. Firebase is gone now (the backend
    // generates this itself, see authController.register), but every other collection already
    // references users via `{ type: String, ref: 'User' }`, so the type stays String to avoid
    // a wider schema migration for no functional benefit.
    _id: { type: String, required: true },

    fullName: { type: String, required: true, trim: true },
    fatherName: { type: String, trim: true },
    cnic: { type: String, required: true, unique: true, trim: true },
    phone: { type: String, required: true, unique: true, trim: true },
    email: { type: String, required: true, unique: true, trim: true, lowercase: true },
    passwordHash: { type: String, required: true, select: false },

    // Forgot-password flow: a hash of the emailed reset code (never the plaintext code
    // itself) plus its expiry. Both cleared once used or superseded by a newer request.
    passwordResetTokenHash: { type: String, default: null, select: false },
    passwordResetExpires: { type: Date, default: null, select: false },

    role: { type: String, enum: Object.values(UserRole), required: true },
    status: { type: String, enum: Object.values(UserStatus), default: UserStatus.PENDING },
    statusHistory: { type: [statusHistoryEntrySchema], default: [] },

    profilePhotoUrl: { type: String, default: null },
    profilePhotoPublicId: { type: String, default: null },

    // FCM registration tokens for this user's devices - an array (not a single field) since
    // the same account can be logged in on more than one device at once. Deduplicated on
    // registration, pruned by fcm.js when a send reports a token as invalid/unregistered.
    fcmTokens: { type: [String], default: [] },
    notificationSettings: { type: notificationSettingsSchema, default: () => ({}) },

    // HOD / STUDENT: single department. TEACHER: multi-department.
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', default: null },
    departmentIds: { type: [{ type: Schema.Types.ObjectId, ref: 'Department' }], default: [] },

    // STUDENT only
    sessionId: { type: Schema.Types.ObjectId, ref: 'Session', default: null },
    currentSemesterId: { type: Schema.Types.ObjectId, ref: 'Semester', default: null },
    registrationNumber: { type: String, default: null },
    rollNumber: { type: String, default: null },
    retakeSubjectIds: { type: [{ type: Schema.Types.ObjectId, ref: 'Subject' }], default: [] },

    // TEACHER only
    employeeId: { type: String, default: null },
    designation: { type: String, default: null },
  },
  { timestamps: true }
);

// Sparse so unplaced students / non-students (null values) don't collide; the unique constraint
// is the concurrency guard that makes server-side generation safe under parallel registrations
// (see services/studentNumbering.js).
userSchema.index({ registrationNumber: 1 }, { unique: true, sparse: true });
userSchema.index({ rollNumber: 1 }, { unique: true, sparse: true });

userSchema.methods.toPublicJSON = function toPublicJSON() {
  const obj = this.toObject({ versionKey: false });
  delete obj.passwordHash;
  return obj;
};

module.exports = mongoose.model('User', userSchema);
