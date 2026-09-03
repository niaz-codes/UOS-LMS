const express = require('express');
const mongoose = require('mongoose');

const router = express.Router();

router.get('/', (req, res) => {
  const dbStates = ['disconnected', 'connected', 'connecting', 'disconnecting'];
  res.json({
    status: 'ok',
    db: dbStates[mongoose.connection.readyState] || 'unknown',
    time: new Date().toISOString(),
  });
});

module.exports = router;
