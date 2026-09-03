const express = require('express');
const { authenticate, requireApproved } = require('../middleware/auth');
const c = require('../controllers/materialController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/', c.create);
router.get('/', c.list);
router.patch('/:id', c.update);
router.delete('/:id', c.remove);

module.exports = router;
