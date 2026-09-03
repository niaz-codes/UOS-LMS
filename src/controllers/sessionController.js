const Session = require('../models/Session');
const User = require('../models/User');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');

const listForDepartment = asyncHandler(async (req, res) => {
  const sessions = await Session.find({ departmentId: req.params.departmentId }).sort({ label: 1 });
  res.json({ sessions });
});

const create = asyncHandler(async (req, res) => {
  const { label, isActive } = req.body;
  if (!label) throw new ApiError(400, 'label is required');
  let session;
  try {
    session = await Session.create({
      departmentId: req.params.departmentId,
      label,
      isActive: isActive === undefined ? true : Boolean(isActive),
    });
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, `A session named "${label}" already exists in this department`);
    throw err;
  }
  res.status(201).json({ session });
});

const update = asyncHandler(async (req, res) => {
  const { label, isActive } = req.body;
  const session = await Session.findById(req.params.id);
  if (!session) throw new ApiError(404, 'Session not found');

  if (label !== undefined) session.label = label;
  if (isActive !== undefined) session.isActive = Boolean(isActive);
  try {
    await session.save();
  } catch (err) {
    if (err.code === 11000) throw new ApiError(409, `A session named "${label}" already exists in this department`);
    throw err;
  }
  res.json({ session });
});

/** Bulk-moves every student from one session to another - the "reassign before delete"
 * step the Admin flow requires when a session still has linked students. */
const reassignStudents = asyncHandler(async (req, res) => {
  const { toSessionId } = req.body;
  if (!toSessionId) throw new ApiError(400, 'toSessionId is required');

  const [fromSession, toSession] = await Promise.all([
    Session.findById(req.params.id),
    Session.findById(toSessionId),
  ]);
  if (!fromSession) throw new ApiError(404, 'Source session not found');
  if (!toSession) throw new ApiError(404, 'Target session not found');
  if (!fromSession.departmentId.equals(toSession.departmentId)) {
    throw new ApiError(400, 'Both sessions must belong to the same department');
  }

  const result = await User.updateMany({ sessionId: fromSession._id }, { sessionId: toSession._id });
  res.json({ movedCount: result.modifiedCount });
});

const remove = asyncHandler(async (req, res) => {
  const session = await Session.findById(req.params.id);
  if (!session) throw new ApiError(404, 'Session not found');

  const linkedStudents = await User.countDocuments({ sessionId: session._id });
  if (linkedStudents > 0) {
    throw new ApiError(400, `Cannot delete: ${linkedStudents} student(s) are still linked to this session. Reassign them first.`);
  }

  const historicalResults = await StudentSemesterResult.countDocuments({ sessionId: session._id });
  if (historicalResults > 0) {
    throw new ApiError(400, `Cannot delete: ${historicalResults} historical semester result(s) reference this session.`);
  }

  await session.deleteOne();
  res.status(204).send();
});

/** Unscoped total across every department - the Admin Student Management tree's header stat. */
const countAll = asyncHandler(async (req, res) => {
  const count = await Session.countDocuments();
  res.json({ count });
});

module.exports = { listForDepartment, create, update, remove, reassignStudents, countAll };
