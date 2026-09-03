require('dotenv').config();
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');

(async () => {
  await connectDb();
  const em = await mongoose.connection.collection('users').find({}, { email: 1, role: 1, phone: 1, cnic: 1 }).sort({ email: 1 }).toArray();
  console.log('total users:', em.length);
  for (const u of em) console.log(u.role, '|', u.email, '| phone:', u.phone, '| cnic:', u.cnic);
  await mongoose.disconnect();
})().catch((e) => { console.error(e); process.exit(1); });
