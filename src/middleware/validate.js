const { validationResult } = require('express-validator');
const { ApiError } = require('./errorHandler');

function validate(req, res, next) {
  const result = validationResult(req);
  if (!result.isEmpty()) {
    const message = result.array().map((e) => e.msg).join('; ');
    return next(new ApiError(400, message));
  }
  next();
}

module.exports = { validate };
