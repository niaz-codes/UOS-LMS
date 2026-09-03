require('dotenv').config();
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');

const User = require('../models/User');
const Department = require('../models/Department');
const Session = require('../models/Session');
const Semester = require('../models/Semester');
const Subject = require('../models/Subject');

const { UserRole, UserStatus } = require('../constants/enums');

// E2E test data seed. Idempotent + non-destructive: every write is an upsert keyed on a
// natural unique field (email / dept code / dept+session / dept+number / dept+subject code).
// Never deletes or overwrites pre-existing accounts (the admin in particular is left
// untouched). The @edu.pk test accounts and the E2E subject codes below are deliberately
// distinct from any production/legacy data so a full teardown can be done safely later.

const SESSION_LABEL = '2024-2028';

const DEPTS = [
  { code: 'CS', name: 'Computer Science' },
  { code: 'IT', name: 'Information Technology' },
];

// E2E cohort subjects per department: 4 for semester 1, 2 for semester 2. Codes chosen so
// they never collide with the existing CS101-104/CS201-202 and IT101-105 rows.
const E2E_SUBJECTS = {
  CS: {
    1: [
      { code: 'CS111', title: 'Programming Fundamentals', creditHours: 3 },
      { code: 'CS112', title: 'Object Oriented Programming', creditHours: 3 },
      { code: 'CS113', title: 'Data Structures & Algorithms', creditHours: 3 },
      { code: 'CS114', title: 'Database Systems', creditHours: 3 },
    ],
    2: [
      { code: 'CS211', title: 'Operating Systems', creditHours: 3 },
      { code: 'CS212', title: 'Computer Networks', creditHours: 3 },
    ],
  },
  IT: {
    1: [
      { code: 'IT111', title: 'Programming Fundamentals', creditHours: 3 },
      { code: 'IT112', title: 'Web Development', creditHours: 3 },
      { code: 'IT113', title: 'Computer Networks', creditHours: 3 },
      { code: 'IT114', title: 'Database Fundamentals', creditHours: 3 },
    ],
    2: [
      { code: 'IT211', title: 'Software Engineering', creditHours: 3 },
      { code: 'IT212', title: 'Human Computer Interaction', creditHours: 3 },
    ],
  },
};

const TOTAL_STUDENTS = 30;
const CS_STUDENT_COUNT = 15;

function pad(value, width) {
  return String(value).padStart(width, '0');
}

function log(msg) {
  // eslint-disable-next-line no-console
  console.log(msg);
}

async function upsertDepartment(code, name) {
  return Department.findOneAndUpdate(
    { code },
    { code, name, description: `Department of ${name}` },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  );
}

async function upsertSession(departmentId, label) {
  return Session.findOneAndUpdate(
    { departmentId, label },
    { departmentId, label, isActive: true },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  );
}

async function upsertSemester(departmentId, number) {
  return Semester.findOneAndUpdate(
    { departmentId, number },
    { departmentId, number },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  );
}

async function upsertSubject(departmentId, semesterId, code, title, creditHours, teacherId) {
  return Subject.findOneAndUpdate(
    { departmentId, code },
    { departmentId, semesterId, code, title, creditHours, teacherId },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  );
}

async function upsertUser(fields) {
  const passwordHash = await bcrypt.hash(fields.password, 10);
  const existing = await User.findOne({ email: fields.email });
  const publicFields = { ...fields };
  delete publicFields.password;
  if (existing) {
    Object.assign(existing, publicFields, { passwordHash });
    await existing.save();
    return existing;
  }
  const created = await User.create({
    _id: new mongoose.Types.ObjectId().toString(),
    passwordHash,
    status: UserStatus.APPROVED,
    statusHistory: [{ status: UserStatus.APPROVED, at: new Date(), reason: 'Seeded E2E test data' }],
    ...publicFields,
  });
  return created;
}

async function main() {
  await connectDb();
  log('Connected. Seeding E2E test data...');

  const state = {};
  for (const { code, name } of DEPTS) {
    const dept = await upsertDepartment(code, name);
    const session = await upsertSession(dept._id, SESSION_LABEL);
    const sem1 = await upsertSemester(dept._id, 1);
    const sem2 = await upsertSemester(dept._id, 2);

    const hodEmail = code === 'CS' ? 'hodcs@edu.pk' : 'hodit@edu.pk';
    const teacherEmail = code === 'CS' ? 'teacher1@edu.pk' : 'teacher2@edu.pk';

    const hod = await upsertUser({
      email: hodEmail,
      password: code === 'CS' ? 'hodcs1' : 'hodit1',
      fullName: `HOD ${name}`,
      fatherName: `Father of HOD ${name}`,
      cnic: code === 'CS' ? '42201-7000001-1' : '42201-7000002-1',
      phone: code === 'CS' ? '03007000001' : '03007000002',
      role: UserRole.HOD,
      departmentId: dept._id,
      employeeId: `EMP-${code}-HOD`,
      designation: 'Head of Department',
    });

    const teacher = await upsertUser({
      email: teacherEmail,
      password: code === 'CS' ? 'teacher1' : 'teacher2',
      fullName: `Teacher ${code}`,
      fatherName: `Father of Teacher ${code}`,
      cnic: code === 'CS' ? '42201-7000003-1' : '42201-7000004-1',
      phone: code === 'CS' ? '03007000003' : '03007000004',
      role: UserRole.TEACHER,
      departmentIds: [dept._id],
      employeeId: `EMP-${code}-T01`,
      designation: 'Lecturer',
    });

    const subjects = {};
    for (const [semNumber, list] of Object.entries(E2E_SUBJECTS[code])) {
      const semester = semNumber === '1' ? sem1 : sem2;
      subjects[semNumber] = [];
      for (const s of list) {
        subjects[semNumber].push(await upsertSubject(dept._id, semester._id, s.code, s.title, s.creditHours, teacher._id));
      }
    }

    state[code] = { dept, session, sem1, sem2, hod, teacher, subjects };
    log(`Dept ${code}: session ${SESSION_LABEL}, semesters 1-2, ${Object.values(subjects).flat().length} subjects, HOD + teacher ready.`);
  }

  // Ownership normalization: legacy session-assignment test rows may have left IT subjects
  // assigned to teacher1 and CS subjects to teacher2. Move every subject of a department to
  // that department's E2E teacher so the workspace/isolation assertions are deterministic.
  const csTeacherId = state.CS.teacher._id.toString();
  const itTeacherId = state.IT.teacher._id.toString();
  await Subject.updateMany({ departmentId: state.IT.dept._id, teacherId: csTeacherId }, { teacherId: itTeacherId });
  await Subject.updateMany({ departmentId: state.CS.dept._id, teacherId: itTeacherId }, { teacherId: csTeacherId });
  log('Subject ownership normalized per department.');

  // 30 students: student1..student15 -> CS, student16..student30 -> IT.
  for (let i = 1; i <= TOTAL_STUDENTS; i += 1) {
    const code = i <= CS_STUDENT_COUNT ? 'CS' : 'IT';
    const s = state[code];
    const seqInDept = i <= CS_STUDENT_COUNT ? i : i - CS_STUDENT_COUNT;
    const student = await upsertUser({
      email: `student${i}@edu.pk`,
      password: `student${i}`,
      fullName: `Student ${i}`,
      fatherName: `Father of Student ${i}`,
      cnic: `42201-9${pad(i, 6)}-1`,
      phone: `03159${pad(i, 6)}`,
      role: UserRole.STUDENT,
      departmentId: s.dept._id,
      sessionId: s.session._id,
      currentSemesterId: s.sem1._id,
      registrationNumber: `UOS-2024-${pad(i, 4)}`,
      rollNumber: `${code}-2024-${pad(seqInDept, 3)}`,
    });
    s.students = s.students || [];
    s.students.push(student);
  }

  log(`Created/verified ${state.CS.students.length} CS students and ${state.IT.students.length} IT students.`);
  log('E2E seed complete.');
  await mongoose.disconnect();
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('Seed failed:', err);
  process.exit(1);
});
