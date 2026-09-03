const express = require('express');
const { authenticate, requireApproved } = require('../middleware/auth');
const { upload: uploadMiddleware } = require('../middleware/upload');
const { upload, replace, remove, getById, removeMyProfilePhoto } = require('../controllers/mediaController');

const router = express.Router();

router.use(authenticate, requireApproved);

router.post('/upload', uploadMiddleware.single('file'), upload);
router.delete('/profile-photo', removeMyProfilePhoto);
router.post('/:id/replace', uploadMiddleware.single('file'), replace);
router.get('/:id', getById);
router.delete('/:id', remove);

module.exports = router;
