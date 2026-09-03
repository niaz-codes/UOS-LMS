const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const attendanceController = require('../controllers/attendanceController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/', requireRole(UserRole.TEACHER, UserRole.HOD, UserRole.ADMIN), attendanceController.save);
router.get('/', attendanceController.list);

module.exports = router;
