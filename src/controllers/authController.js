const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const User = require('../models/User');
const Department = require('../models/Department');
const Session = require('../models/Session');
const Semester = require('../models/Semester');
const { signToken } = require('../services/jwt');
const { sendMail } = require('../services/email');
const { validateImageFile, saveProfilePhoto } = require('../services/cloudinaryUpload');
const { notifyUsers, notifyAfterResponse, resolveAllAdmins } = require('../services/notificationService');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { generateRegistrationNumber, assignRollNumber } = require('../services/studentNumbering');
const { UserRole, REGISTERABLE_ROLES, UserStatus, NotificationType } = require('../constants/enums');

const PASSWORD_RESET_TTL_MINUTES = 30;

function hashResetCode(code) {
  return crypto.createHash('sha256').update(code).digest('hex');
}

const DUPLICATE_FIELD_LABELS = { email: 'Email', cnic: 'CNIC', phone: 'Phone number', _id: 'This account' };

/** Multipart bodies arrive with a single value as a string and repeated values as an array -
 * normalize so a single ObjectId or a list both work for multi-select fields. */
function toArray(value) {
  if (value === undefined || value === null) return [];
  return Array.isArray(value) ? value : [value];
}

function toIdOrNull(value) {
  if (value === undefined || value === null) return null;
  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
}

function mapDuplicateKeyError(err) {
  if (err && err.code === 11000 && err.keyPattern) {
    const field = Object.keys(err.keyPattern)[0];
    const label = DUPLICATE_FIELD_LABELS[field] || field;
    return new ApiError(409, `${label} is already registered`);
  }
  return null;
}

function isValidObjectId(value) {
  return typeof value === 'string' && mongoose.Types.ObjectId.isValid(value);
}

async function requireExistingDoc(Model, id, label) {
  if (!isValidObjectId(id)) throw new ApiError(400, `Invalid ${label} selection`);
  const doc = await Model.findById(id);
  if (!doc) throw new ApiError(400, `Selected ${label} was not found`);
  return doc;
}

/** Enforces the exact role-based academic selection rules the registration form must collect
 * (see the Registration Screen spec): STUDENT needs department + session + semester, all three
 * of which must actually exist and the session/semester must belong to the chosen department;
 * TEACHER needs at least one department; HOD needs exactly one department. Every id is checked
 * against the database, not just presence, so a stale or mistyped id can never silently create
 * an orphaned academic reference. */
async function validateAcademicSelection(role, roleFields) {
  if (role === UserRole.STUDENT) {
    const departmentId = toIdOrNull(roleFields.departmentId);
    const sessionId = toIdOrNull(roleFields.sessionId);
    const semesterId = toIdOrNull(roleFields.currentSemesterId);
    if (!departmentId) throw new ApiError(400, 'departmentId is required for STUDENT registration');
    if (!sessionId) throw new ApiError(400, 'sessionId is required for STUDENT registration');
    if (!semesterId) throw new ApiError(400, 'currentSemesterId is required for STUDENT registration');

    const department = await requireExistingDoc(Department, departmentId, 'department');
    const session = await requireExistingDoc(Session, sessionId, 'session');
    const semester = await requireExistingDoc(Semester, semesterId, 'semester');
    if (session.departmentId.toString() !== department._id.toString()) {
      throw new ApiError(400, 'The selected session does not belong to the selected department');
    }
    if (semester.departmentId.toString() !== department._id.toString()) {
      throw new ApiError(400, 'The selected semester does not belong to the selected department');
    }
    return;
  }

  if (role === UserRole.TEACHER) {
    const departmentIds = toArray(roleFields.departmentIds).map(toIdOrNull).filter(Boolean);
    if (departmentIds.length === 0) {
      throw new ApiError(400, 'At least one departmentId is required for TEACHER registration');
    }
    for (const id of departmentIds) {
      // eslint-disable-next-line no-await-in-loop
      await requireExistingDoc(Department, id, 'department');
    }
    return;
  }

  if (role === UserRole.HOD) {
    const departmentId = toIdOrNull(roleFields.departmentId);
    if (!departmentId) throw new ApiError(400, 'departmentId is required for HOD registration');
    await requireExistingDoc(Department, departmentId, 'department');
  }
}

const register = asyncHandler(async (req, res) => {
  const { fullName, fatherName, cnic, phone, email, password, role, ...roleFields } = req.body;

  if (!REGISTERABLE_ROLES.includes(role)) {
    throw new ApiError(400, `role must be one of: ${REGISTERABLE_ROLES.join(', ')}`);
  }

  // A malformed/oversized photo is a client input error - fail before creating the account
  // so we never end up with a user stuck half-registered. A working upload that later fails
  // (network blip, Cloudinary outage) is handled separately, after the account exists, and
  // is non-fatal - see below.
  if (req.file) {
    validateImageFile(req.file);
  }

  await validateAcademicSelection(role, roleFields);

  const passwordHash = await bcrypt.hash(password, 10);

  const userDoc = {
    // The backend owns identity generation now (no more client-supplied Firebase uid) -
    // ObjectId().toString() just for a familiar id shape, the schema field itself is a
    // plain String (see User.js).
    _id: new mongoose.Types.ObjectId().toString(),
    fullName,
    fatherName,
    cnic,
    phone,
    email,
    passwordHash,
    role,
    status: UserStatus.PENDING,
    statusHistory: [{ status: UserStatus.PENDING, at: new Date() }],
  };

  if (role === UserRole.TEACHER) {
    userDoc.employeeId = roleFields.employeeId || null;
    userDoc.designation = roleFields.designation || null;
    userDoc.departmentIds = toArray(roleFields.departmentIds);
  }
  if (role === UserRole.HOD) {
    userDoc.departmentId = toIdOrNull(roleFields.departmentId);
  }
  if (role === UserRole.STUDENT) {
    userDoc.departmentId = toIdOrNull(roleFields.departmentId);
    userDoc.sessionId = toIdOrNull(roleFields.sessionId);
    userDoc.currentSemesterId = toIdOrNull(roleFields.currentSemesterId);
  }

  // The backend owns student numbering: the registration number is always generated here. If
  // the applicant already picked a department + session on the form, the roll number follows
  // right below; otherwise it's assigned at academic placement (see studentNumbering.js).
  let user;
  for (let attempt = 0; attempt < 5; attempt += 1) {
    if (role === UserRole.STUDENT) {
      userDoc.registrationNumber = await generateRegistrationNumber();
      userDoc.rollNumber = null;
    }
    try {
      user = await User.create(userDoc);
      break;
    } catch (err) {
      // Two registrations racing for the same number - the sparse unique index caught it,
      // regenerate and retry. Any other duplicate (email/cnic/phone) surfaces as a 409.
      if (err.code === 11000 && err.keyPattern && err.keyPattern.registrationNumber) continue;
      throw mapDuplicateKeyError(err) || err;
    }
  }
  if (!user) {
    throw new ApiError(500, 'Could not assign a unique registration number. Please try again.');
  }

  // A student registering with a department + session already forms a complete cohort, so the
  // roll number can (and should) be assigned right away - placement afterwards only matters
  // for changes. assignRollNumber is a no-op until both are present.
  if (user.role === UserRole.STUDENT) {
    await assignRollNumber(user, { placementChanged: true });
  }

  if (req.file) {
    try {
      const media = await saveProfilePhoto(user._id, req.file);
      user.profilePhotoUrl = media.secureUrl;
      user.profilePhotoPublicId = media.publicId;
      await user.save();
    } catch (err) {
      // The account already exists at this point - don't fail registration over a photo
      // upload hiccup. The user can add/retry their photo later from the Profile screen.
      // eslint-disable-next-line no-console
      console.error('Registration photo upload failed:', err.message);
    }
  }

  res.status(201).json({ user: user.toPublicJSON() });

  notifyAfterResponse(async () => {
    const adminIds = await resolveAllAdmins();
    await notifyUsers(adminIds, {
      type: NotificationType.ACCOUNT_REGISTERED,
      title: `New registration: ${user.fullName}`,
      body: `${role} - awaiting approval`,
      actorId: user._id,
      relatedType: 'user',
      relatedId: user._id,
    });
  });
});

const STATUS_MESSAGES = {
  [UserStatus.PENDING]: 'Your account is awaiting admin approval.',
  [UserStatus.REJECTED]: 'Your registration was rejected.',
  [UserStatus.SUSPENDED]: 'Your account has been suspended.',
};

const login = asyncHandler(async (req, res) => {
  const { email, password } = req.body;

  const user = await User.findOne({ email: email.trim().toLowerCase() }).select('+passwordHash');
  if (!user) {
    throw new ApiError(401, 'Invalid email or password');
  }

  const passwordMatches = await bcrypt.compare(password, user.passwordHash);
  if (!passwordMatches) {
    throw new ApiError(401, 'Invalid email or password');
  }

  if (user.status !== UserStatus.APPROVED) {
    throw new ApiError(403, STATUS_MESSAGES[user.status] || 'Account is not approved', {
      accountStatus: user.status,
    });
  }

  const token = signToken(user);
  res.json({ token, user: user.toPublicJSON() });
});

const me = asyncHandler(async (req, res) => {
  res.json({ user: req.user.toPublicJSON() });
});

// Keeps the Mongo User doc's denormalized photo fields in sync with the Media collection
// (see mediaController) after a profile-photo upload/delete. Self-service only - a user
// can only ever update their own record here (req.user._id, not a body-supplied id).
const updateMyPhoto = asyncHandler(async (req, res) => {
  const { profilePhotoUrl, profilePhotoPublicId } = req.body;
  req.user.profilePhotoUrl = profilePhotoUrl || null;
  req.user.profilePhotoPublicId = profilePhotoPublicId || null;
  await req.user.save();
  res.json({ user: req.user.toPublicJSON() });
});

/** Always responds the same way whether or not the email is registered, so a caller can't
 * use this endpoint to probe which emails have accounts (same intent as Firebase's Email
 * Enumeration Protection, which this replaces). Email delivery failure is logged server-side
 * but not surfaced to the client for the same reason. */
const forgotPassword = asyncHandler(async (req, res) => {
  const email = req.body.email.trim().toLowerCase();
  const user = await User.findOne({ email });

  if (user) {
    const code = String(crypto.randomInt(100000, 1000000));
    user.passwordResetTokenHash = hashResetCode(code);
    user.passwordResetExpires = new Date(Date.now() + PASSWORD_RESET_TTL_MINUTES * 60 * 1000);
    await user.save();

    try {
      await sendMail({
        to: user.email,
        subject: 'UOS LMS - Password Reset Code',
        text: `Your password reset code is ${code}. It expires in ${PASSWORD_RESET_TTL_MINUTES} minutes.\n\nIf you didn't request this, you can safely ignore this email.`,
        html: `<p>Your password reset code is <b style="font-size:20px">${code}</b>.</p>`
          + `<p>It expires in ${PASSWORD_RESET_TTL_MINUTES} minutes.</p>`
          + `<p>If you didn't request this, you can safely ignore this email.</p>`,
      });
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('Failed to send password reset email:', err.message);
    }
  }

  res.json({ message: 'If an account exists for that email, a reset code has been sent.' });
});

const resetPassword = asyncHandler(async (req, res) => {
  const email = req.body.email.trim().toLowerCase();
  const { code, newPassword } = req.body;

  const user = await User.findOne({ email }).select('+passwordResetTokenHash +passwordResetExpires');
  const invalid = !user
    || !user.passwordResetTokenHash
    || !user.passwordResetExpires
    || user.passwordResetExpires.getTime() < Date.now()
    || hashResetCode(code) !== user.passwordResetTokenHash;

  if (invalid) {
    throw new ApiError(400, 'That reset code is invalid or has expired. Please request a new one.');
  }

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  user.passwordResetTokenHash = null;
  user.passwordResetExpires = null;
  await user.save();

  res.json({ message: 'Password reset successfully. You can now log in with your new password.' });
});

/** Self-service, requires the current password (unlike resetPassword, which is for when the
 * user has forgotten it and instead proves ownership via the emailed code). */
const changePassword = asyncHandler(async (req, res) => {
  const { currentPassword, newPassword } = req.body;
  if (!currentPassword || !newPassword) {
    throw new ApiError(400, 'currentPassword and newPassword are required');
  }
  if (newPassword.length < 6) {
    throw new ApiError(400, 'newPassword must be at least 6 characters');
  }

  const user = await User.findById(req.user._id).select('+passwordHash');
  const matches = await bcrypt.compare(currentPassword, user.passwordHash);
  if (!matches) {
    throw new ApiError(401, 'Current password is incorrect');
  }

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  await user.save();

  res.json({ message: 'Password changed successfully.' });

  notifyAfterResponse(() => notifyUsers([user._id], {
    type: NotificationType.PASSWORD_CHANGED,
    title: 'Your password was changed',
    body: '',
    relatedType: 'user',
    relatedId: user._id,
  }));
});

module.exports = { register, login, me, updateMyPhoto, forgotPassword, resetPassword, changePassword };
