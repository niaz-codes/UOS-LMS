const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const c = require('../controllers/assignmentController');

const router = express.Router();

router.use(authenticate, requireApproved);

const { ADMIN, HOD, TEACHER, STUDENT } = UserRole;

router.post('/', requireRole(TEACHER, ADMIN), c.createAssignment);
router.get('/', c.listAssignments);
router.delete('/:id', requireRole(ADMIN), c.removeAssignment);
router.post('/:assignmentId/submissions', requireRole(STUDENT), c.submit);

router.get('/submissions', c.listSubmissions);
router.patch('/submissions/:id/grade', requireRole(TEACHER, HOD, ADMIN), c.grade);

module.exports = router;
