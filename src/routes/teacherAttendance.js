const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const teacherAttendanceController = require('../controllers/teacherAttendanceController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/', requireRole(UserRole.HOD, UserRole.ADMIN), teacherAttendanceController.save);
router.get('/', requireRole(UserRole.HOD, UserRole.ADMIN, UserRole.TEACHER), teacherAttendanceController.list);

module.exports = router;
