const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const c = require('../controllers/leaveController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/', requireRole(UserRole.STUDENT), c.apply);
router.get('/', c.list);
router.post('/:id/approve', requireRole(UserRole.TEACHER, UserRole.HOD, UserRole.ADMIN), c.approve);
router.post('/:id/reject', requireRole(UserRole.TEACHER, UserRole.HOD, UserRole.ADMIN), c.reject);

module.exports = router;
