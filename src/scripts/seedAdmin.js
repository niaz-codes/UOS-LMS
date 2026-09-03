require('dotenv').config();
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');
const User = require('../models/User');
const { UserRole, UserStatus } = require('../constants/enums');

async function seedAdmin() {
  const email = process.env.SEED_ADMIN_EMAIL || 'niazkhan4371@gmail.com';
  const password = process.env.SEED_ADMIN_PASSWORD || 'Admin@12345';
  const fullName = process.env.SEED_ADMIN_NAME || 'System Administrator';

  await connectDb();

  const existing = await User.findOne({ email: email.toLowerCase() });
  if (existing) {
    console.log(`Admin already exists: ${email}`);
    await mongoose.disconnect();
    return;
  }

  const passwordHash = await bcrypt.hash(password, 10);
  await User.create({
    // No matching Firebase Auth account exists for this bootstrap admin (it's created
    // directly in MongoDB, bypassing the app's register flow) — so any screen still
    // reading Firestore/FirebaseAuth during the transitional migration won't recognize
    // this account. Fine for backend API testing; for full in-app testing, register a
    // normal account through the app and promote it via PATCH /api/users/:id/role instead.
    _id: process.env.SEED_ADMIN_UID || 'seed-admin-bootstrap',
    fullName,
    cnic: process.env.SEED_ADMIN_CNIC || '00000-0000000-0',
    phone: process.env.SEED_ADMIN_PHONE || '0000000000',
    email: email.toLowerCase(),
    passwordHash,
    role: UserRole.ADMIN,
    status: UserStatus.APPROVED,
    statusHistory: [{ status: UserStatus.APPROVED, at: new Date() }],
  });

  console.log(`Admin created: ${email} / ${password} (change this password after first login)`);
  await mongoose.disconnect();
}

seedAdmin()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Failed to seed admin:', err);
    process.exit(1);
  });
