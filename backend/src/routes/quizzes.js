const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');
const c = require('../controllers/quizController');

const router = express.Router();

router.use(authenticate, requireApproved);

const { ADMIN, HOD, TEACHER, STUDENT } = UserRole;

router.post('/', requireRole(TEACHER, ADMIN), c.create);
router.get('/', c.list);
router.get('/attempts', c.listAttempts);
router.get('/:id', c.getById);
router.delete('/:id', requireRole(ADMIN), c.remove);
router.post('/:id/start', requireRole(STUDENT), c.startAttempt);
router.post('/:id/attempts', requireRole(STUDENT), c.submitAttempt);
router.patch('/attempts/:id/answers', requireRole(STUDENT), c.saveProgress);
router.patch('/attempts/:id/grade', requireRole(TEACHER, ADMIN), c.gradeAttempt);

module.exports = router;
