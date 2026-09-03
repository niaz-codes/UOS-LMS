const Department = require('../models/Department');
const Session = require('../models/Session');
const Semester = require('../models/Semester');
const User = require('../models/User');
const { UserRole } = require('../constants/enums');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');

const list = asyncHandler(async (req, res) => {
  const departments = await Department.find().sort({ name: 1 });
  res.json({ departments });
});

const create = asyncHandler(async (req, res) => {
  const { name, code, description } = req.body;
  if (!name || !code) {
    throw new ApiError(400, 'name and code are required');
  }
  let department;
  try {
    department = await Department.create({ name, code, description: description || null });
  } catch (err) {
    if (err.code === 11000) {
      throw new ApiError(409, 'A department with this code already exists');
    }
    throw err;
  }
  res.status(201).json({ department });
});

const update = asyncHandler(async (req, res) => {
  const { name, code, description } = req.body;
  const department = await Department.findById(req.params.id);
  if (!department) throw new ApiError(404, 'Department not found');

  if (name !== undefined) department.name = name;
  if (code !== undefined) department.code = code;
  if (description !== undefined) department.description = description;

  try {
    await department.save();
  } catch (err) {
    if (err.code === 11000) {
      throw new ApiError(409, 'A department with this code already exists');
    }
    throw err;
  }
  res.json({ department });
});

const remove = asyncHandler(async (req, res) => {
  const department = await Department.findById(req.params.id);
  if (!department) throw new ApiError(404, 'Department not found');

  const [semesterCount, sessionCount, hodCount, teacherCount, studentCount] = await Promise.all([
    Semester.countDocuments({ departmentId: department._id }),
    Session.countDocuments({ departmentId: department._id }),
    User.countDocuments({ role: UserRole.HOD, departmentId: department._id }),
    User.countDocuments({ role: UserRole.TEACHER, departmentIds: department._id }),
    User.countDocuments({ role: UserRole.STUDENT, departmentId: department._id }),
  ]);
  if (semesterCount > 0 || sessionCount > 0) {
    throw new ApiError(400, 'Cannot delete a department that still has semesters or sessions');
  }
  if (hodCount > 0 || teacherCount > 0 || studentCount > 0) {
    throw new ApiError(400, `Cannot delete: this department still has ${hodCount} HOD(s), ${teacherCount} teacher(s), and ${studentCount} student(s) assigned. Reassign or remove them first.`);
  }

  await department.deleteOne();
  res.status(204).send();
});

module.exports = { list, create, update, remove };
