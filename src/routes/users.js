const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const {
  list,
  getById,
  updateStatus,
  updateDepartment,
  updateAcademicPlacement,
  updateProfile,
  updateIdentifiers,
  counts,
  hodForDepartment,
  updateRole,
  remove,
  joinDepartment,
  contacts,
} = require('../controllers/userController');

const router = express.Router();

router.use(authenticate);

router.get('/', requireRole(UserRole.ADMIN, UserRole.HOD), list);
router.get('/counts', requireRole(UserRole.ADMIN, UserRole.HOD), counts);
router.get('/messaging-contacts', requireApproved, contacts);
router.get('/hod-for-department/:departmentId', requireRole(UserRole.ADMIN), hodForDepartment);
router.post('/me/departments/:departmentId', joinDepartment);
router.get('/:id', getById);
router.patch('/:id/status', requireRole(UserRole.ADMIN), updateStatus);
router.patch('/:id/department', requireRole(UserRole.ADMIN), updateDepartment);
router.patch('/:id/academic', requireRole(UserRole.ADMIN), updateAcademicPlacement);
router.patch('/:id/profile', updateProfile);
router.patch('/:id/identifiers', requireRole(UserRole.ADMIN), updateIdentifiers);
router.patch('/:id/role', requireRole(UserRole.ADMIN), updateRole);
router.delete('/:id', requireRole(UserRole.ADMIN), remove);

module.exports = router;
