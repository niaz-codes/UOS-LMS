const User = require('../models/User');
const Department = require('../models/Department');
const Session = require('../models/Session');
const { ApiError } = require('../middleware/errorHandler');
const { UserRole } = require('../constants/enums');

// Sequence settings live in the environment so the starting number (and digit width) can be
// tuned without touching code. The year normally tracks the current calendar year, but can be
// pinned when admissions for an upcoming session open before January (see .env.example).
const REGISTRATION_NUMBER_YEAR = process.env.REGISTRATION_NUMBER_YEAR || null;
const REGISTRATION_SEQUENCE_START = parseInt(process.env.REGISTRATION_SEQUENCE_START || '1', 10);
const REGISTRATION_SEQUENCE_PADDING = parseInt(process.env.REGISTRATION_SEQUENCE_PADDING || '4', 10);
const ROLL_NUMBER_SEQUENCE_START = parseInt(process.env.ROLL_NUMBER_SEQUENCE_START || '1', 10);
const ROLL_NUMBER_SEQUENCE_PADDING = parseInt(process.env.ROLL_NUMBER_SEQUENCE_PADDING || '3', 10);

const MAX_NUMBERING_ATTEMPTS = 5;

function pad(value, width) {
  return String(value).padStart(width, '0');
}

function registrationYear() {
  if (REGISTRATION_NUMBER_YEAR) return REGISTRATION_NUMBER_YEAR;
  return String(new Date().getFullYear());
}

/** UOS-style global admission number, e.g. "UOS-2026-0001". The department/session are NOT
 * known at registration time (they're assigned later via academic placement), so unlike the
 * roll number this one is scoped by admission year only, never per-cohort. */
function buildRegistrationNumber(sequence) {
  return `UOS-${registrationYear()}-${pad(sequence, REGISTRATION_SEQUENCE_PADDING)}`;
}

/** Highest trailing sequence already in use among docs matching `query` (0 if none). Parses
 * numerically instead of string-sorting because roll numbers across data generations can have
 * different digit widths (seed "CS-2023-01" vs generated "CS-2023-001"). */
async function maxUsedSequence(query, field) {
  const docs = await User.find(query, { [field]: 1 }).lean();
  let max = 0;
  for (const doc of docs) {
    const value = doc[field];
    if (typeof value !== 'string') continue;
    const match = value.match(/(\d+)\s*$/);
    if (!match) continue;
    const number = parseInt(match[1], 10);
    if (number > max) max = number;
  }
  return max;
}

/** Next registration number for the current admission year, computed from the highest number
 * already handed out. Not persisted here - the caller persists it with a retry loop on the
 * unique-index collision so two concurrent registrations can never end up sharing one. */
async function generateRegistrationNumber() {
  const prefix = `UOS-${registrationYear()}-`;
  const max = await maxUsedSequence(
    { role: UserRole.STUDENT, registrationNumber: { $regex: `^${prefix}` } },
    'registrationNumber'
  );
  return buildRegistrationNumber(Math.max(REGISTRATION_SEQUENCE_START, max + 1));
}

/** Start year of a session label ("2023-2027" -> "2023"), or null when it doesn't begin
 * with a 4-digit year. */
function rollYearFor(session) {
  if (!session || !session.label) return null;
  const match = String(session.label).match(/^(\d{4})/);
  return match ? match[1] : null;
}

/** Roll-number prefix for a cohort, e.g. "CS-2023-". */
function rollPrefix(department, session) {
  const year = rollYearFor(session);
  if (!department || !department.code || !year) return null;
  return `${department.code}-${year}-`;
}

/** Ensures `user` (a STUDENT) has a roll number matching their current department + session,
 * e.g. "CS-2023-001". Called whenever either is (re)assigned. The sequence counts existing
 * students in the same cohort (department + session), so the number is stable per cohort even
 * as semesters progress. A manually-edited custom roll number is never clobbered unless the
 * placement actually changed. */
async function assignRollNumber(user, { placementChanged = false } = {}) {
  if (user.role !== UserRole.STUDENT || !user.departmentId || !user.sessionId) return;

  const [department, session] = await Promise.all([
    Department.findById(user.departmentId),
    Session.findById(user.sessionId),
  ]);
  const prefix = rollPrefix(department, session);
  if (!prefix) return;

  const matchesCohort = !!user.rollNumber && user.rollNumber.startsWith(prefix);
  if (matchesCohort) return;
  if (user.rollNumber && !placementChanged) return;

  const max = await maxUsedSequence(
    {
      role: UserRole.STUDENT,
      departmentId: user.departmentId,
      sessionId: user.sessionId,
      rollNumber: { $regex: `^${prefix}` },
    },
    'rollNumber'
  );
  let sequence = Math.max(ROLL_NUMBER_SEQUENCE_START, max + 1);

  for (let attempt = 0; attempt < MAX_NUMBERING_ATTEMPTS; attempt += 1) {
    user.rollNumber = `${prefix}${pad(sequence, ROLL_NUMBER_SEQUENCE_PADDING)}`;
    try {
      await user.save();
      return;
    } catch (err) {
      // A parallel placement landed on the same number first - bump and retry.
      if (err.code === 11000) {
        sequence += 1;
        continue;
      }
      throw err;
    }
  }
  throw new ApiError(500, 'Could not assign a unique roll number. Please try again.');
}

module.exports = { generateRegistrationNumber, assignRollNumber, buildRegistrationNumber, registrationYear };
