const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const c = require('../controllers/teacherLeaveController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/', requireRole(UserRole.TEACHER), c.apply);
router.get('/', c.list);
router.post('/:id/approve', requireRole(UserRole.HOD, UserRole.ADMIN), c.approve);
router.post('/:id/reject', requireRole(UserRole.HOD, UserRole.ADMIN), c.reject);

module.exports = router;
