const express = require('express');
const departmentController = require('../controllers/departmentController');
const sessionController = require('../controllers/sessionController');
const semesterController = require('../controllers/semesterController');

const router = express.Router();

// Public (pre-auth) read-only catalog endpoints used by the registration screen to populate the
// Department / Session / Semester options. These expose only the curriculum catalog a sign-up
// form needs - no user data, no auth required. The authenticated equivalents live in
// university.js for the logged-in Admin/HOD flows.
router.get('/departments', departmentController.list);
router.get('/departments/:departmentId/sessions', sessionController.listForDepartment);
router.get('/departments/:departmentId/semesters', semesterController.listForDepartment);

module.exports = router;
