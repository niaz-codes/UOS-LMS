const Semester = require('../models/Semester');
const Subject = require('../models/Subject');
const User = require('../models/User');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');

const listForDepartment = asyncHandler(async (req, res) => {
  const semesters = await Semester.find({ departmentId: req.params.departmentId }).sort({ number: 1 });
  res.json({ semesters });
});

const create = asyncHandler(async (req, res) => {
  const { number } = req.body;
  if (!number || number < 1) throw new ApiError(400, 'number is required and must be >= 1');

  let semester;
  try {
    semester = await Semester.create({ departmentId: req.params.departmentId, number });
  } catch (err) {
    if (err.code === 11000) {
      throw new ApiError(409, `Semester ${number} already exists for this department`);
    }
    throw err;
  }
  res.status(201).json({ semester });
});

const remove = asyncHandler(async (req, res) => {
  const semester = await Semester.findById(req.params.id);
  if (!semester) throw new ApiError(404, 'Semester not found');

  const subjectCount = await Subject.countDocuments({ semesterId: semester._id });
  if (subjectCount > 0) {
    throw new ApiError(400, 'Cannot delete a semester that still has subjects');
  }

  const enrolledStudents = await User.countDocuments({ currentSemesterId: semester._id });
  if (enrolledStudents > 0) {
    throw new ApiError(400, `Cannot delete: ${enrolledStudents} student(s) are currently placed in this semester. Promote or reassign them first.`);
  }

  await semester.deleteOne();
  res.status(204).send();
});

/** Unscoped total across every department - the Admin Student Management tree's header stat. */
const countAll = asyncHandler(async (req, res) => {
  const count = await Semester.countDocuments();
  res.json({ count });
});

module.exports = { listForDepartment, create, remove, countAll };
