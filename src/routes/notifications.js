const express = require('express');
const { body } = require('express-validator');
const { authenticate, requireApproved } = require('../middleware/auth');
const { validate } = require('../middleware/validate');
const {
  list,
  unreadCount,
  markRead,
  markAllRead,
  remove,
  getSettings,
  updateSettings,
  registerFcmToken,
  unregisterFcmToken,
} = require('../controllers/notificationController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.get('/notifications', list);
router.get('/notifications/unread-count', unreadCount);
router.patch('/notifications/read-all', markAllRead);
router.patch('/notifications/:id/read', markRead);
router.delete('/notifications/:id', remove);

router.get('/notifications/settings', getSettings);
router.put('/notifications/settings', updateSettings);

router.post(
  '/notifications/fcm-token',
  [body('token').trim().notEmpty().withMessage('token is required')],
  validate,
  registerFcmToken
);
router.delete(
  '/notifications/fcm-token',
  [body('token').trim().notEmpty().withMessage('token is required')],
  validate,
  unregisterFcmToken
);

module.exports = router;
