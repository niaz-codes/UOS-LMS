const express = require('express');
const { authenticate, requireApproved } = require('../middleware/auth');
const announcementController = require('../controllers/announcementController');
const calendarController = require('../controllers/calendarController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/announcements', announcementController.create);
router.get('/announcements', announcementController.list);
router.delete('/announcements/:id', announcementController.remove);

router.post('/calendar-events', calendarController.create);
router.get('/calendar-events', calendarController.list);
router.delete('/calendar-events/:id', calendarController.remove);

module.exports = router;
