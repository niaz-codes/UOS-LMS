const express = require('express');
const { authenticate, requireApproved } = require('../middleware/auth');
const c = require('../controllers/messagingController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/conversations', c.findOrCreate);
router.get('/conversations', c.listConversations);
router.post('/conversations/:id/messages', c.sendMessage);
router.get('/conversations/:id/messages', c.listMessages);
router.post('/conversations/:id/read', c.markRead);
router.get('/messages/mine', c.listMyMessages);

module.exports = router;
