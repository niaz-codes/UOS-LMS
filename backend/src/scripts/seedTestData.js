require('dotenv').config();
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const { connectDb } = require('../config/db');

const User = require('../models/User');
const Department = require('../models/Department');
const Session = require('../models/Session');
const Semester = require('../models/Semester');
const Subject = require('../models/Subject');
const AttendanceRecord = require('../models/AttendanceRecord');
const Assignment = require('../models/Assignment');
const AssignmentSubmission = require('../models/AssignmentSubmission');
const Quiz = require('../models/Quiz');
const QuizAttempt = require('../models/QuizAttempt');
const ExamResult = require('../models/ExamResult');
const RepeatExam = require('../models/RepeatExam');
const Promotion = require('../models/Promotion');
const StudentSemesterResult = require('../models/StudentSemesterResult');
const Announcement = require('../models/Announcement');
const CalendarEvent = require('../models/CalendarEvent');
const StudyMaterial = require('../models/StudyMaterial');
const Media = require('../models/Media');
const LeaveApplication = require('../models/LeaveApplication');
const Message = require('../models/Message');
const Conversation = require('../models/Conversation');
const TimetableSlot = require('../models/TimetableSlot');
const ExamSchedule = require('../models/ExamSchedule');

const { UserRole, UserStatus, ResultStatus, PromotionStatus, RepeatStatus, MediaCategory, MAX_ALLOWED_FAILED_SUBJECTS } = require('../constants/enums');
const { gradeFor } = require('../services/gradeScale');
const { recalculateSemesterGpa } = require('../services/recalculateSemesterGpa');

const ADMIN_EMAIL = 'niazkhan4371@gmail.com';

const DEPTS = [
  { code: 'CS', name: 'Computer Science' },
  { code: 'SE', name: 'Software Engineering' },
  { code: 'EE', name: 'Electrical Engineering' },
  { code: 'BBA', name: 'Business Administration' },
  { code: 'MATH', name: 'Mathematics' },
];

const SESSION_LABEL = '2023-2027';
const SUBJECT_TITLES = ['Introduction to the Field', 'Core Principles', 'Applied Methods', 'Professional Practice'];
const NEXT_SEM_TITLES = ['Advanced Topics I', 'Advanced Topics II'];

function log(msg) {
  // eslint-disable-next-line no-console
  console.log(msg);
}

async function upsertUser(doc) {
  const passwordHash = await bcrypt.hash(doc.password, 10);
  const existing = await User.findOne({ email: doc.email });
  if (existing) {
    Object.assign(existing, doc.fields, { passwordHash });
    await existing.save();
    return existing;
  }
  const created = await User.create({
    _id: new mongoose.Types.ObjectId().toString(),
    passwordHash,
    status: UserStatus.APPROVED,
    statusHistory: [{ status: UserStatus.APPROVED, at: new Date(), reason: 'Seeded test data' }],
    ...doc.fields,
  });
  return created;
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

async function upsertPlaceholderMedia(category, uploadedBy, relatedId, fileName) {
  // Deliberately NOT a real Cloudinary asset - a structurally valid placeholder URL under the
  // real cloud_name, matching the spec's "placeholder URLs if needed" allowance. The real
  // upload path (mediaController/Cloudinary) is already covered by the app's own upload
  // screens - this is test *data*, not a re-test of the upload pipeline.
  const publicId = `${category}/seed/${relatedId}`;
  return Media.findOneAndUpdate(
    { category, uploadedBy, 'relatedTo.id': relatedId },
    {
      secureUrl: `https://res.cloudinary.com/vh8k7ctw/raw/upload/${publicId}`,
      publicId,
      resourceType: 'raw',
      fileName,
      fileType: 'application/pdf',
      fileSize: 102400,
      category,
      uploadedBy,
      relatedTo: { type: 'subject', id: relatedId },
    },
    { new: true, upsert: true, setDefaultsOnInsert: true }
  );
}

function dateKeyDaysAgo(days) {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

function daysFromNow(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d;
}

async function main() {
  await connectDb();
  log('Connected. Seeding test data...');

  // ---- Admin (upsert only - never overwrite an existing admin's password/role) ----
  let admin = await User.findOne({ email: ADMIN_EMAIL });
  if (!admin) {
    admin = await upsertUser({
      password: 'Admin@12345',
      email: ADMIN_EMAIL,
      fields: {
        fullName: 'System Administrator', cnic: '00000-0000000-0', phone: '0000000000',
        email: ADMIN_EMAIL, role: UserRole.ADMIN,
      },
    });
    log(`Created admin ${ADMIN_EMAIL}`);
  } else {
    log(`Admin ${ADMIN_EMAIL} already exists - left untouched.`);
  }

  const departments = [];
  const sessions = [];
  const semester1s = [];
  const semester2s = [];
  const hods = [];
  const teachers = [];
  const students = [];
  const subjects1 = []; // subjects1[i] = array of 4 Subject docs for department i, semester 1
  const subjects2 = []; // subjects1[i] = array of 2 Subject docs for department i, semester 2

  for (let i = 0; i < DEPTS.length; i += 1) {
    const n = i + 1;
    const { code, name } = DEPTS[i];

    const dept = await upsertDepartment(code, name);
    departments.push(dept);

    const session = await upsertSession(dept._id, SESSION_LABEL);
    sessions.push(session);

    const sem1 = await upsertSemester(dept._id, 1);
    const sem2 = await upsertSemester(dept._id, 2);
    semester1s.push(sem1);
    semester2s.push(sem2);

    const hod = await upsertUser({
      password: `uoshod${n}`,
      email: `HOD${n}@gmail.com`.toLowerCase(),
      fields: {
        fullName: `HOD ${n}`, fatherName: `Father of HOD ${n}`,
        cnic: `11101-111110${n}-1`, phone: `030111110${n}${n}`,
        email: `HOD${n}@gmail.com`.toLowerCase(), role: UserRole.HOD,
        departmentId: dept._id, employeeId: `EMP-${code}-HOD`, designation: 'Head of Department',
      },
    });
    hods.push(hod);

    const teacher = await upsertUser({
      password: `uosteacher${n}`,
      email: `teacher${n}@gmail.com`,
      fields: {
        fullName: `Teacher ${n}`, fatherName: `Father of Teacher ${n}`,
        cnic: `22202-222220${n}-2`, phone: `030222220${n}${n}`,
        email: `teacher${n}@gmail.com`, role: UserRole.TEACHER,
        departmentIds: [dept._id], employeeId: `EMP-${code}-T01`, designation: 'Lecturer',
      },
    });
    teachers.push(teacher);

    const student = await upsertUser({
      password: `uosstudent${n}`,
      email: `student${n}@gmail.com`,
      fields: {
        fullName: `Student ${n}`, fatherName: `Father of Student ${n}`,
        cnic: `33303-333330${n}-3`, phone: `030333330${n}${n}`,
        email: `student${n}@gmail.com`, role: UserRole.STUDENT,
        departmentId: dept._id, sessionId: session._id, currentSemesterId: sem1._id,
        registrationNumber: `2023-${code}-01`, rollNumber: `${code}-2023-01`,
      },
    });
    students.push(student);

    const subs1 = [];
    for (let s = 0; s < SUBJECT_TITLES.length; s += 1) {
      subs1.push(await upsertSubject(dept._id, sem1._id, `${code}10${s + 1}`, SUBJECT_TITLES[s], 3, teacher._id));
    }
    subjects1.push(subs1);

    const subs2 = [];
    for (let s = 0; s < NEXT_SEM_TITLES.length; s += 1) {
      subs2.push(await upsertSubject(dept._id, sem2._id, `${code}20${s + 1}`, NEXT_SEM_TITLES[s], 3, teacher._id));
    }
    subjects2.push(subs2);

    log(`Department ${code}: HOD/Teacher/Student + ${subs1.length + subs2.length} subjects ready.`);
  }

  // ---- Per-department academic data: attendance, assignments, quizzes, exam results ----
  // Department index 4 (Student 5 / MATH) deliberately fails every subject to exercise the
  // 4-failed-subjects promotion block + repeat-exam workflow; departments 0-3 pass everything
  // and get fully promoted, so the seed leaves both a "successfully promoted" and an
  // "in-progress repeat exam" example to click through.
  const FAILING_DEPT_INDEX = 4;

  for (let i = 0; i < DEPTS.length; i += 1) {
    const dept = departments[i];
    const teacher = teachers[i];
    const student = students[i];
    const sem1 = semester1s[i];
    const isFailingCohort = i === FAILING_DEPT_INDEX;

    for (let s = 0; s < subjects1[i].length; s += 1) {
      const subject = subjects1[i][s];

      // Attendance: 8 sessions, one absence for realism (two for the failing cohort).
      for (let d = 0; d < 8; d += 1) {
        const dateKey = dateKeyDaysAgo(8 - d);
        const status = (isFailingCohort ? [2, 5] : [3]).includes(d) ? 'ABSENT' : 'PRESENT';
        await AttendanceRecord.findOneAndUpdate(
          { subjectId: subject._id, dateKey, studentId: student._id },
          { subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id, dateKey, studentId: student._id, status, markedBy: teacher._id },
          { upsert: true, setDefaultsOnInsert: true }
        );
      }

      // Assignment + one graded submission.
      const assignment = await Assignment.findOneAndUpdate(
        { subjectId: subject._id, title: 'Assignment 1' },
        {
          subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id,
          title: 'Assignment 1', description: `${subject.title} - Assignment 1`,
          dueDate: daysFromNow(-2), maxMarks: 20, createdBy: teacher._id,
        },
        { new: true, upsert: true, setDefaultsOnInsert: true }
      );
      const assignmentMarks = isFailingCohort ? 8 : 17;
      await AssignmentSubmission.findOneAndUpdate(
        { assignmentId: assignment._id, studentId: student._id },
        {
          assignmentId: assignment._id, subjectId: subject._id, departmentId: dept._id, studentId: student._id,
          textAnswer: `${student.fullName}'s submission for ${subject.title}.`,
          submittedAt: daysFromNow(-3), marksObtained: assignmentMarks, feedback: isFailingCohort ? 'Needs significant improvement.' : 'Good work.',
          gradedBy: teacher._id, gradedAt: daysFromNow(-1),
        },
        { upsert: true, setDefaultsOnInsert: true }
      );

      // Quiz + one attempt (auto-scored, matches the app's own server-side scoring).
      const questions = [
        { text: `${subject.title}: Question 1`, options: ['Option A', 'Option B', 'Option C', 'Option D'], correctOptionIndex: 0, marks: 5 },
        { text: `${subject.title}: Question 2`, options: ['Option A', 'Option B', 'Option C', 'Option D'], correctOptionIndex: 1, marks: 5 },
        { text: `${subject.title}: Question 3`, options: ['Option A', 'Option B', 'Option C', 'Option D'], correctOptionIndex: 2, marks: 5 },
      ];
      const quiz = await Quiz.findOneAndUpdate(
        { subjectId: subject._id, title: 'Quiz 1' },
        {
          subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id,
          title: 'Quiz 1', description: `${subject.title} - Quiz 1`, type: 'QUIZ',
          questions, timeLimitMinutes: 15, dueDate: daysFromNow(-1), createdBy: teacher._id,
        },
        { new: true, upsert: true, setDefaultsOnInsert: true }
      );
      const rawAnswers = isFailingCohort ? [1, 0, 0] : [0, 1, 2];
      const score = questions.reduce((sum, q, idx) => sum + (rawAnswers[idx] === q.correctOptionIndex ? q.marks : 0), 0);
      const answers = rawAnswers.map((optionIndex) => ({ optionIndex, textAnswer: null }));
      await QuizAttempt.findOneAndUpdate(
        { quizId: quiz._id, studentId: student._id },
        { quizId: quiz._id, subjectId: subject._id, studentId: student._id, answers, score, totalMarks: 15, startedAt: daysFromNow(-1), submittedAt: daysFromNow(-1) },
        { upsert: true, setDefaultsOnInsert: true }
      );

      // Exam result: submit -> approve (with audit log, exactly like the real controller).
      const marks = isFailingCohort ? 35 + s : 72 + s * 4; // varied, deliberately all-F vs all-pass
      const { grade, gpa, status } = gradeFor(marks);
      let examResult = await ExamResult.findOne({ studentId: student._id, subjectId: subject._id });
      if (!examResult) {
        examResult = new ExamResult({
          studentId: student._id, subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id,
          marks, grade, gpa, status, resultStatus: ResultStatus.PENDING_HOD_APPROVAL,
          repeatEligible: status === 'FAIL', submittedBy: teacher._id,
        });
      } else {
        examResult.marks = marks;
        examResult.grade = grade;
        examResult.gpa = gpa;
        examResult.status = status;
        examResult.repeatEligible = status === 'FAIL';
      }
      if (examResult.resultStatus !== ResultStatus.APPROVED) {
        const previousValues = { resultStatus: examResult.resultStatus, marks: examResult.marks, grade: examResult.grade, gpa: examResult.gpa };
        examResult.resultStatus = ResultStatus.APPROVED;
        examResult.reviewedBy = hods[i]._id;
        examResult.reviewedAt = new Date();
        examResult.auditLog.push({ action: 'APPROVE', by: hods[i]._id, at: new Date(), previousValues, newValues: { resultStatus: ResultStatus.APPROVED } });
      }
      await examResult.save();

      // Study material + timetable slot + exam schedule for this subject.
      const media = await upsertPlaceholderMedia(MediaCategory.STUDY_MATERIAL, teacher._id, subject._id, `${subject.code}-notes.pdf`);
      await StudyMaterial.findOneAndUpdate(
        { subjectId: subject._id, title: `${subject.title} - Lecture Notes` },
        {
          subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id,
          title: `${subject.title} - Lecture Notes`, materialType: 'PDF',
          mediaId: media._id, fileUrl: media.secureUrl, fileName: media.fileName,
          filePublicId: media.publicId, fileResourceType: media.resourceType, fileSize: media.fileSize,
          uploadedBy: teacher._id,
        },
        { upsert: true, setDefaultsOnInsert: true }
      );

      const room = `Room-${dept.code}-10${s + 1}`;
      await TimetableSlot.findOneAndUpdate(
        { departmentId: dept._id, semesterId: sem1._id, dayOfWeek: TimetableSlot.DAYS[s % 5], startTimeMinutes: 540 + s * 90 },
        {
          departmentId: dept._id, semesterId: sem1._id, subjectId: subject._id, teacherId: teacher._id,
          dayOfWeek: TimetableSlot.DAYS[s % 5], startTimeMinutes: 540 + s * 90, endTimeMinutes: 540 + s * 90 + 60,
          room, roomNormalized: room.toUpperCase(), createdBy: admin._id,
        },
        { upsert: true, setDefaultsOnInsert: true }
      );

      const examDate = daysFromNow(21 + s);
      const examRoom = `Exam-Hall-${dept.code}-${s + 1}`;
      await ExamSchedule.findOneAndUpdate(
        { subjectId: subject._id, examType: 'MID_TERM' },
        {
          examType: 'MID_TERM', subjectId: subject._id, departmentId: dept._id, semesterId: sem1._id,
          teacherId: teacher._id, invigilatorId: teacher._id, room: examRoom, roomNormalized: examRoom.toUpperCase(),
          examDate, examDateKey: examDate.toISOString().slice(0, 10), startTimeMinutes: 540 + s * 60, endTimeMinutes: 540 + s * 60 + 60,
          status: 'PUBLISHED', createdBy: admin._id, publishedAt: new Date(),
        },
        { upsert: true, setDefaultsOnInsert: true }
      );
    }

    // Recompute this student's semester rollup from the ExamResults just approved - the same
    // single reusable function every real approval path calls, never hand-computed here.
    await recalculateSemesterGpa(student._id, sem1._id);
    log(`Department ${dept.code}: attendance/assignments/quizzes/exam results/materials/timetable/exam-schedule seeded.`);
  }

  // ---- Repeat exam for the failing student's first failed subject, then re-recalculate ----
  const failingStudent = students[FAILING_DEPT_INDEX];
  const failingDept = departments[FAILING_DEPT_INDEX];
  const failingSem1 = semester1s[FAILING_DEPT_INDEX];
  const failingTeacher = teachers[FAILING_DEPT_INDEX];
  const failingHod = hods[FAILING_DEPT_INDEX];
  const firstFailedResult = await ExamResult.findOne({ studentId: failingStudent._id, subjectId: subjects1[FAILING_DEPT_INDEX][0]._id });

  let repeatExam = await RepeatExam.findOne({ examResultId: firstFailedResult._id });
  if (!repeatExam) {
    repeatExam = await RepeatExam.create({
      examResultId: firstFailedResult._id, studentId: failingStudent._id, subjectId: firstFailedResult.subjectId,
      departmentId: failingDept._id, previousMarks: firstFailedResult.marks, previousGrade: firstFailedResult.grade, previousGpa: firstFailedResult.gpa,
      repeatStatus: RepeatStatus.PENDING,
    });
  }
  if (repeatExam.repeatStatus === RepeatStatus.PENDING) {
    const passingMarks = 68;
    const { grade, gpa } = gradeFor(passingMarks);
    repeatExam.newMarks = passingMarks;
    repeatExam.newGrade = grade;
    repeatExam.newGpa = gpa;
    repeatExam.repeatStatus = RepeatStatus.SUBMITTED;
    repeatExam.submittedBy = failingTeacher._id;
    await repeatExam.save();

    const previousValues = { marks: firstFailedResult.marks, grade: firstFailedResult.grade, gpa: firstFailedResult.gpa, status: firstFailedResult.status };
    firstFailedResult.marks = repeatExam.newMarks;
    firstFailedResult.grade = repeatExam.newGrade;
    firstFailedResult.gpa = repeatExam.newGpa;
    firstFailedResult.status = 'PASS';
    firstFailedResult.repeatEligible = false;
    firstFailedResult.auditLog.push({ action: 'REPEAT_EXAM_APPROVED', by: failingHod._id, at: new Date(), previousValues, newValues: { marks: firstFailedResult.marks, grade: firstFailedResult.grade, gpa: firstFailedResult.gpa, status: 'PASS' } });
    await firstFailedResult.save();

    repeatExam.repeatStatus = RepeatStatus.APPROVED;
    repeatExam.reviewedBy = failingHod._id;
    repeatExam.reviewedAt = new Date();
    await repeatExam.save();

    await recalculateSemesterGpa(failingStudent._id, failingSem1._id);
    log(`Repeat exam for ${failingStudent.email} approved (3 of 4 subjects still failed -> now ELIGIBLE_FOR_PROMOTION, left un-promoted for manual testing).`);
  }

  // ---- Promote the 4 successful students into semester 2 ----
  for (let i = 0; i < DEPTS.length; i += 1) {
    if (i === FAILING_DEPT_INDEX) continue; // left eligible-but-unpromoted on purpose, see above
    const student = students[i];
    const sem1 = semester1s[i];
    const sem2 = semester2s[i];
    const semesterResult = await StudentSemesterResult.findOne({ studentId: student._id, semesterId: sem1._id });
    if (semesterResult.promotionStatus === PromotionStatus.PROMOTED) continue;

    const already = await Promotion.findOne({ studentId: student._id, fromSemesterId: sem1._id });
    if (!already) {
      await Promotion.create({
        studentId: student._id, fromSemesterId: sem1._id, toSemesterId: sem2._id,
        promotedBy: admin._id, failedSubjectIds: semesterResult.failedSubjectIds,
      });
    }
    student.currentSemesterId = sem2._id;
    await student.save();
    semesterResult.promotionStatus = PromotionStatus.PROMOTED;
    await semesterResult.save();
    log(`Promoted ${student.email} to semester 2.`);
  }

  // ---- Announcements ----
  await Announcement.findOneAndUpdate(
    { title: 'Welcome to the 2023-2027 Session' },
    { title: 'Welcome to the 2023-2027 Session', body: 'Welcome to all students, faculty, and staff for the new academic session.', authorId: admin._id, scope: 'ALL' },
    { upsert: true, setDefaultsOnInsert: true }
  );
  for (let i = 0; i < DEPTS.length; i += 1) {
    await Announcement.findOneAndUpdate(
      { title: `${DEPTS[i].name} Department Notice`, departmentId: departments[i]._id },
      { title: `${DEPTS[i].name} Department Notice`, body: `Departmental orientation for ${DEPTS[i].name} students will be held this week.`, authorId: hods[i]._id, scope: 'DEPARTMENT', departmentId: departments[i]._id },
      { upsert: true, setDefaultsOnInsert: true }
    );
  }

  // ---- Calendar events ----
  const events = [
    { title: 'Mid-Term Examinations Begin', type: 'EXAM', date: daysFromNow(21) },
    { title: 'Public Holiday', type: 'HOLIDAY', date: daysFromNow(10) },
    { title: 'Orientation Week', type: 'EVENT', date: daysFromNow(-5) },
  ];
  for (const event of events) {
    await CalendarEvent.findOneAndUpdate(
      { title: event.title },
      { ...event, description: event.title, createdBy: admin._id },
      { upsert: true, setDefaultsOnInsert: true }
    );
  }

  // ---- Leave applications: 4 approved, 1 left pending for manual testing ----
  for (let i = 0; i < DEPTS.length; i += 1) {
    const pending = i === 0;
    await LeaveApplication.findOneAndUpdate(
      { studentId: students[i]._id, reason: 'Medical leave' },
      {
        studentId: students[i]._id, departmentId: departments[i]._id, semesterId: semester1s[i]._id,
        fromDate: daysFromNow(-2), toDate: daysFromNow(1), reason: 'Medical leave',
        status: pending ? 'PENDING' : 'APPROVED',
        reviewerId: pending ? null : hods[i]._id, reviewerRole: pending ? null : UserRole.HOD,
        decidedAt: pending ? null : daysFromNow(-1),
      },
      { upsert: true, setDefaultsOnInsert: true }
    );
  }

  // ---- Messaging: Student<->Teacher, HOD<->Teacher (same dept), Admin<->HOD ----
  async function seedConversation(userA, userB, texts) {
    const [a, b] = [userA._id.toString(), userB._id.toString()].sort();
    const conversationId = `${a}_${b}`;
    let conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      conversation = await Conversation.create({ _id: conversationId, participantAId: a, participantBId: b });
    }
    for (const [senderId, text] of texts) {
      const recipientId = senderId === a ? b : a;
      const existing = await Message.findOne({ conversationId, text, senderId });
      if (!existing) {
        await Message.create({ conversationId, senderId, recipientId, text, sentAt: new Date() });
      }
    }
    const last = texts[texts.length - 1];
    conversation.lastMessageText = last[1];
    conversation.lastMessageSenderId = last[0];
    conversation.lastMessageAt = new Date();
    await conversation.save();
  }

  for (let i = 0; i < DEPTS.length; i += 1) {
    await seedConversation(students[i], teachers[i], [
      [students[i]._id.toString(), 'Hello, I had a question about the last lecture.'],
      [teachers[i]._id.toString(), 'Sure, happy to help - what would you like to know?'],
    ]);
    await seedConversation(hods[i], teachers[i], [
      [hods[i]._id.toString(), 'Please submit your exam results by Friday.'],
    ]);
  }
  await seedConversation(admin, hods[0], [
    [admin._id.toString(), 'Please review the new semester schedule.'],
  ]);

  log('Test data seed complete.');
  await mongoose.disconnect();
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('Seed failed:', err);
  process.exit(1);
});
