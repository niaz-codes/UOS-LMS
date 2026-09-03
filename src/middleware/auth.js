const { verifyToken } = require('../services/jwt');
const { ApiError } = require('./errorHandler');
const User = require('../models/User');
const { UserStatus } = require('../constants/enums');

async function authenticate(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    const [scheme, token] = header.split(' ');
    if (scheme !== 'Bearer' || !token) {
      throw new ApiError(401, 'Missing or malformed Authorization header');
    }

    let payload;
    try {
      payload = verifyToken(token);
    } catch (err) {
      throw new ApiError(401, 'Invalid or expired token');
    }

    const user = await User.findById(payload.sub);
    if (!user) {
      throw new ApiError(401, 'User no longer exists');
    }

    req.user = user;
    next();
  } catch (err) {
    next(err);
  }
}

function requireApproved(req, res, next) {
  if (!req.user || req.user.status !== UserStatus.APPROVED) {
    return next(new ApiError(403, 'Account is not approved'));
  }
  next();
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return next(new ApiError(403, 'Insufficient role for this action'));
    }
    next();
  };
}

module.exports = { authenticate, requireApproved, requireRole };
