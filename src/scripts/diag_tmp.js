require('dotenv').config();
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');

(async () => {
  await connectDb();
  const d = await mongoose.connection.collection('departments').find({ code: { $in: ['CS', 'IT'] } }).toArray();
  for (const x of d) {
    console.log('===', x.code, x._id.toString());
    const s = await mongoose.connection.collection('sessions').find({ departmentId: x._id }).toArray();
    for (const ss of s) console.log('  session:', ss.label, ss._id.toString(), 'active:', ss.isActive);
    const sems = await mongoose.connection.collection('semesters').find({ departmentId: x._id }).sort({ number: 1 }).toArray();
    for (const sem of sems) console.log('  semester:', sem.number, sem._id.toString());
    const subs = await mongoose.connection.collection('subjects').find({ departmentId: x._id }).toArray();
    for (const sub of subs.sort((a, b) => a.code.localeCompare(b.code))) {
      console.log('  subject:', sub.code, sub.title, 'sem:', sub.semesterId.toString(), 'teacher:', sub.teacherId || 'null');
    }
  }
  const st = await mongoose.connection.collection('users').find({
    email: { $in: ['stdu1@edu.pk', 'stdu2@edu.pk', 'stdu3@edu.pk', 'stdu4@edu.pk', 'teacher1@edu.pk', 'teacher2@edu.pk', 'hodcs@edu.pk', 'hodit@edu.pk'] },
  }).toArray();
  for (const u of st) {
    console.log('USER', u.email, u.role, 'dept:', u.departmentId, 'deptIds:', u.departmentIds, 'session:', u.sessionId, 'sem:', u.currentSemesterId, 'roll:', u.rollNumber, 'reg:', u.registrationNumber);
  }
  await mongoose.disconnect();
})().catch((e) => { console.error(e); process.exit(1); });
