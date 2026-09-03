require('dotenv').config();

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const { connectDb } = require('./config/db');
const healthRouter = require('./routes/health');
const authRouter = require('./routes/auth');
const publicRouter = require('./routes/public');
const usersRouter = require('./routes/users');
const mediaRouter = require('./routes/media');
const universityRouter = require('./routes/university');
const gradingRouter = require('./routes/grading');
const attendanceRouter = require('./routes/attendance');
const teacherAttendanceRouter = require('./routes/teacherAttendance');
const assignmentsRouter = require('./routes/assignments');
const quizzesRouter = require('./routes/quizzes');
const contentRouter = require('./routes/content');
const materialsRouter = require('./routes/materials');
const leaveRouter = require('./routes/leave');
const teacherLeaveRouter = require('./routes/teacherLeave');
const schedulingRouter = require('./routes/scheduling');
const messagingRouter = require('./routes/messaging');
const notificationsRouter = require('./routes/notifications');
const { notFoundHandler, errorHandler } = require('./middleware/errorHandler');

const app = express();

// Railway (like Render/Heroku) terminates TLS at a reverse proxy in front of the app, so
// Express needs to trust its X-Forwarded-* headers to see the real client IP/protocol
// (used by morgan's request logging and by req.secure/req.ip generally).
app.set('trust proxy', 1);

// CORS_ORIGIN (comma-separated) lets deployments restrict which web origins may call the
// API. Left unset, the API allows any origin - the safe default here since this backend is
// primarily consumed by the Android app (which doesn't send/enforce an Origin header) and
// there's no first-party web frontend yet.
const configuredOrigins = (process.env.CORS_ORIGIN || '')
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);
const corsOptions = configuredOrigins.length > 0 ? { origin: configuredOrigins } : { origin: true };

app.use(helmet());
app.use(cors(corsOptions));
app.use(express.json());
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));

app.use('/health', healthRouter);
app.use('/api/auth', authRouter);
app.use('/api/public', publicRouter);
app.use('/api/users', usersRouter);
app.use('/api/media', mediaRouter);
app.use('/api', universityRouter);
app.use('/api', gradingRouter);
app.use('/api/attendance', attendanceRouter);
app.use('/api/teacher-attendance', teacherAttendanceRouter);
app.use('/api/assignments', assignmentsRouter);
app.use('/api/quizzes', quizzesRouter);
app.use('/api', contentRouter);
app.use('/api/materials', materialsRouter);
app.use('/api/leaves', leaveRouter);
app.use('/api/teacher-leaves', teacherLeaveRouter);
app.use('/api', schedulingRouter);
app.use('/api', messagingRouter);
app.use('/api', notificationsRouter);

app.use(notFoundHandler);
app.use(errorHandler);

const PORT = process.env.PORT || 4000;

connectDb()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`UOS_LMS backend listening on port ${PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to connect to MongoDB, exiting.', err);
    process.exit(1);
  });

module.exports = app;
