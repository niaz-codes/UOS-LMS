const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');

const examResultController = require('../controllers/examResultController');
const examResultWorkspaceController = require('../controllers/examResultWorkspaceController');
const repeatExamController = require('../controllers/repeatExamController');
const promotionController = require('../controllers/promotionController');

const router = express.Router();

router.use(authenticate, requireApproved);

const { ADMIN, HOD, TEACHER } = UserRole;

// Exam results
router.post('/results/draft', requireRole(TEACHER, ADMIN), examResultController.saveDraft);
router.post('/results/submit', requireRole(TEACHER, ADMIN), examResultController.submit);
router.post('/results/:id/approve', requireRole(HOD, ADMIN), examResultController.approve);
router.post('/results/:id/reject', requireRole(HOD, ADMIN), examResultController.reject);
router.get('/results', examResultController.list);
router.get('/semester-results', examResultController.listSemesterResults);

// Dedicated Teacher Exam Result workspace (options -> subjects -> roster)
router.get('/exam-result/options', requireRole(TEACHER), examResultWorkspaceController.getOptions);
router.get('/exam-result/subjects', requireRole(TEACHER), examResultWorkspaceController.listSubjects);
router.get('/exam-result/roster', requireRole(TEACHER), examResultWorkspaceController.listRoster);

// Repeat exams
router.post('/repeat-exams', requireRole(TEACHER, HOD, ADMIN), repeatExamController.create);
router.patch('/repeat-exams/:id/marks', requireRole(TEACHER, ADMIN), repeatExamController.submitMarks);
router.post('/repeat-exams/:id/approve', requireRole(HOD, ADMIN), repeatExamController.approve);
router.post('/repeat-exams/:id/reject', requireRole(HOD, ADMIN), repeatExamController.reject);
router.get('/repeat-exams/student/:studentId', repeatExamController.listForStudent);
router.get('/repeat-exams', requireRole(TEACHER, HOD, ADMIN), repeatExamController.list);

// Promotion + dashboard
router.post('/students/:id/promote', requireRole(HOD, ADMIN), promotionController.promote);
router.get('/students/:id/dashboard', promotionController.dashboard);

module.exports = router;
