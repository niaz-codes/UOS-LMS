const express = require('express');
const { authenticate, requireApproved, requireRole } = require('../middleware/auth');
const { UserRole } = require('../constants/enums');

const departmentController = require('../controllers/departmentController');
const sessionController = require('../controllers/sessionController');
const semesterController = require('../controllers/semesterController');
const subjectController = require('../controllers/subjectController');

const router = express.Router();

router.use(authenticate, requireApproved);

const ADMIN = UserRole.ADMIN;

// Departments
router.get('/departments', departmentController.list);
router.post('/departments', requireRole(ADMIN), departmentController.create);
router.patch('/departments/:id', requireRole(ADMIN), departmentController.update);
router.delete('/departments/:id', requireRole(ADMIN), departmentController.remove);

// Sessions (cohorts), scoped under a department
router.get('/sessions/count', sessionController.countAll);
router.get('/departments/:departmentId/sessions', sessionController.listForDepartment);
router.post('/departments/:departmentId/sessions', requireRole(ADMIN), sessionController.create);
router.patch('/sessions/:id', requireRole(ADMIN), sessionController.update);
router.post('/sessions/:id/reassign-students', requireRole(ADMIN), sessionController.reassignStudents);
router.delete('/sessions/:id', requireRole(ADMIN), sessionController.remove);

// Semesters (curriculum), scoped under a department
router.get('/semesters/count', semesterController.countAll);
router.get('/departments/:departmentId/semesters', semesterController.listForDepartment);
router.post('/departments/:departmentId/semesters', requireRole(ADMIN), semesterController.create);
router.delete('/semesters/:id', requireRole(ADMIN), semesterController.remove);

// Subjects
router.get('/semesters/:semesterId/subjects', subjectController.listForSemester);
router.get('/departments/:departmentId/subjects', subjectController.listForDepartment);
router.get('/teachers/:teacherId/subjects', subjectController.listForTeacher);
router.get('/subjects/mine', requireRole(UserRole.STUDENT), subjectController.mine);
router.get('/teachers/me/students', requireRole(UserRole.TEACHER), subjectController.myStudents);
router.get('/subjects/:id/roster', subjectController.roster);
router.post('/subjects', requireRole(ADMIN), subjectController.create);
router.patch('/subjects/:id', requireRole(ADMIN), subjectController.update);
router.delete('/subjects/:id', requireRole(ADMIN), subjectController.remove);
router.patch('/subjects/:id/assign-teacher', requireRole(ADMIN, UserRole.HOD), subjectController.assignTeacher);
router.post('/subjects/:id/self-assign', requireRole(UserRole.TEACHER), subjectController.selfAssign);

module.exports = router;
