require('dotenv').config();
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');

(async () => {
  await connectDb();
  const us = await mongoose.connection.collection('users').find({ phone: { $regex: '^03159' } }, { phone: 1, email: 1, role: 1 }).toArray();
  console.log('03159 owners:', us.length);
  for (const u of us) console.log(' ', u.phone, u.email, u.role);
  const cs = await mongoose.connection.collection('users').find({}, { cnic: 1, email: 1 }).toArray();
  const cnics = [...new Set(cs.map((u) => u.cnic))].sort();
  console.log('CNICs:', cnics.join(' | '));
  await mongoose.disconnect();
})().catch((e) => { console.error(e); process.exit(1); });
