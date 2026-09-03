const express = require('express');
const { body } = require('express-validator');
const { register, login, me, updateMyPhoto, forgotPassword, resetPassword, changePassword } = require('../controllers/authController');
const { validate } = require('../middleware/validate');
const { authenticate } = require('../middleware/auth');
const { upload: uploadMiddleware } = require('../middleware/upload');

const router = express.Router();

router.post(
  '/register',
  // Optional - only kicks in for multipart/form-data requests (a photo-less registration
  // can still post plain JSON exactly as before). req.body is populated by multer before
  // the express-validator checks below run, so field validation still works either way.
  uploadMiddleware.single('photo'),
  [
    body('fullName').trim().notEmpty().withMessage('fullName is required'),
    body('cnic').trim().notEmpty().withMessage('cnic is required'),
    body('phone').trim().notEmpty().withMessage('phone is required'),
    body('email').isEmail().withMessage('a valid email is required').normalizeEmail(),
    body('password').isLength({ min: 6 }).withMessage('password must be at least 6 characters'),
    body('role').notEmpty().withMessage('role is required'),
  ],
  validate,
  register
);

router.post(
  '/login',
  [
    body('email').isEmail().withMessage('a valid email is required').normalizeEmail(),
    body('password').notEmpty().withMessage('password is required'),
  ],
  validate,
  login
);

router.get('/me', authenticate, me);
router.patch('/me/photo', authenticate, updateMyPhoto);
router.patch(
  '/me/password',
  authenticate,
  [body('newPassword').isLength({ min: 6 }).withMessage('newPassword must be at least 6 characters')],
  validate,
  changePassword
);

router.post(
  '/forgot-password',
  [body('email').isEmail().withMessage('a valid email is required').normalizeEmail()],
  validate,
  forgotPassword
);

router.post(
  '/reset-password',
  [
    body('email').isEmail().withMessage('a valid email is required').normalizeEmail(),
    body('code').trim().notEmpty().withMessage('code is required'),
    body('newPassword').isLength({ min: 6 }).withMessage('newPassword must be at least 6 characters'),
  ],
  validate,
  resetPassword
);

module.exports = router;
