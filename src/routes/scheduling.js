const express = require('express');
const { authenticate, requireApproved } = require('../middleware/auth');
const timetableController = require('../controllers/timetableController');
const examScheduleController = require('../controllers/examScheduleController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/timetable-slots', timetableController.create);
router.get('/timetable-slots', timetableController.list);
router.delete('/timetable-slots/:id', timetableController.remove);

router.post('/exam-schedules', examScheduleController.create);
router.get('/exam-schedules', examScheduleController.list);
router.post('/exam-schedules/:id/publish', examScheduleController.publish);
router.post('/exam-schedules/:id/lock', examScheduleController.lock);
router.delete('/exam-schedules/:id', examScheduleController.remove);

module.exports = router;
