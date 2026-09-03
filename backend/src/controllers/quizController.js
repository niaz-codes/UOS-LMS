const Quiz = require('../models/Quiz');
const QuizAttempt = require('../models/QuizAttempt');
const Subject = require('../models/Subject');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { notifyUsers, notifyAfterResponse, resolveStudentsInCohort } = require('../services/notificationService');

const POPULATE_STUDENT = { path: 'studentId', select: 'fullName rollNumber registrationNumber' };

async function requireOwnSubject(req, subjectId) {
  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');
  const isAssignedTeacher = subject.teacherId && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can do this');
  }
  return subject;
}

/** Never let a Student see correctOptionIndex before they've attempted the quiz - the
 * pre-migration app trusted the client for auto-grading entirely; this backend computes
 * the score itself (see submitAttempt) and hides the answer key until it's safe to reveal. */
function stripAnswerKey(quiz) {
  const obj = quiz.toObject({ virtuals: true });
  obj.questions = obj.questions.map(({ correctOptionIndex, ...rest }) => rest);
  return obj;
}

function validateQuestion(q, index) {
  if (!q.text || !q.marks || q.marks < 1) {
    throw new ApiError(400, `Question ${index + 1} needs text and marks >= 1`);
  }
  if (q.questionType === 'TEXT') return;
  if (!Array.isArray(q.options) || q.options.length < 2 || typeof q.correctOptionIndex !== 'number'
    || q.correctOptionIndex < 0 || q.correctOptionIndex >= q.options.length) {
    throw new ApiError(400, `Question ${index + 1} needs at least 2 options and a valid correctOptionIndex`);
  }
}

/** The timer's real deadline: whichever comes first, the quiz's overall due date or this
 * student's own start time plus the time limit. Anchored to the server-recorded startedAt
 * (see startAttempt) rather than anything client-supplied, so re-opening the app can't
 * reset the countdown back to full time. */
function deadlineMillis(quiz, attempt) {
  const perAttemptDeadline = attempt.startedAt.getTime() + quiz.timeLimitMinutes * 60_000;
  return Math.min(quiz.dueDate.getTime(), perAttemptDeadline);
}

const create = asyncHandler(async (req, res) => {
  const { subjectId, title, description, questions, timeLimitMinutes, dueDate } = req.body;
  if (!subjectId || !title || !Array.isArray(questions) || questions.length === 0 || !timeLimitMinutes || !dueDate) {
    throw new ApiError(400, 'subjectId, title, questions (non-empty), timeLimitMinutes, and dueDate are required');
  }
  questions.forEach(validateQuestion);
  const subject = await requireOwnSubject(req, subjectId);

  const quiz = await Quiz.create({
    subjectId,
    departmentId: subject.departmentId,
    semesterId: subject.semesterId,
    title,
    description: description || '',
    // The Quiz section only ever creates QUIZ now - EXAM stays as a legacy read-only type
    // for pre-existing documents (see list()/gradeAttempt below), never producible here.
    type: 'QUIZ',
    questions,
    timeLimitMinutes,
    dueDate,
    createdBy: req.user._id.toString(),
  });
  res.status(201).json({ quiz });

  notifyAfterResponse(async () => {
    const recipientIds = await resolveStudentsInCohort(subject.departmentId, subject.semesterId);
    await notifyUsers(recipientIds, {
      type: NotificationType.QUIZ_CREATED,
      title: `New quiz: ${quiz.title}`,
      body: `${subject.title || subject.code || ''} - due ${new Date(quiz.dueDate).toDateString()}`,
      actorId: req.user._id.toString(),
      relatedType: 'quiz',
      relatedId: quiz._id.toString(),
    });
  });
});

const list = asyncHandler(async (req, res) => {
  const { subjectId, departmentId } = req.query;
  // The Quiz section shows Quizzes only - legacy EXAM-type documents (see the `type` field's
  // comment in models/Quiz.js) are never listed here, though they're left untouched in the
  // database and still reachable via getById/grading for anyone who already has the link.
  const filter = { type: 'QUIZ' };
  if (subjectId) filter.subjectId = subjectId;
  if (req.user.role === UserRole.HOD) filter.departmentId = req.user.departmentId;
  else if (departmentId) filter.departmentId = departmentId;

  const quizzes = await Quiz.find(filter).sort({ dueDate: -1 });
  if (req.user.role === UserRole.STUDENT) {
    return res.json({ quizzes: quizzes.map(stripAnswerKey) });
  }
  res.json({ quizzes });
});

const getById = asyncHandler(async (req, res) => {
  const quiz = await Quiz.findById(req.params.id);
  if (!quiz) throw new ApiError(404, 'Quiz not found');

  if (req.user.role === UserRole.STUDENT) {
    const attempt = await QuizAttempt.findOne({ quizId: quiz._id, studentId: req.user._id.toString() });
    // Only reveal the answer key once the attempt is actually submitted - an in-progress
    // attempt (created by startAttempt, before the student has finished) must not leak
    // correctOptionIndex mid-quiz.
    return res.json({ quiz: attempt && attempt.submittedAt ? quiz : stripAnswerKey(quiz) });
  }
  res.json({ quiz });
});

/** Admin-only, permanent - deletes the quiz and every student attempt against it (no orphaned
 * QuizAttempt rows left pointing at a quizId that no longer exists). Not exposed to
 * Teacher/HOD - only the Admin Quiz Monitor screen offers this. */
const remove = asyncHandler(async (req, res) => {
  const quiz = await Quiz.findById(req.params.id);
  if (!quiz) throw new ApiError(404, 'Quiz not found');

  await QuizAttempt.deleteMany({ quizId: quiz._id });
  await quiz.deleteOne();

  res.status(204).send();
});

/** Get-or-create the student's attempt and anchor the countdown to its startedAt - see
 * deadlineMillis. Idempotent: calling this again mid-quiz (app reopened) or after
 * submission just returns the existing attempt as-is, never resetting anything. */
const startAttempt = asyncHandler(async (req, res) => {
  const quiz = await Quiz.findById(req.params.id);
  if (!quiz) throw new ApiError(404, 'Quiz not found');

  let attempt = await QuizAttempt.findOne({ quizId: quiz._id, studentId: req.user._id.toString() });
  if (!attempt) {
    if (quiz.dueDate.getTime() < Date.now()) {
      throw new ApiError(400, 'This quiz is closed - the due date has passed');
    }
    attempt = await QuizAttempt.create({
      quizId: quiz._id,
      subjectId: quiz.subjectId,
      studentId: req.user._id.toString(),
      answers: [],
      score: 0,
      totalMarks: quiz.totalMarks,
      startedAt: new Date(),
    });
  }
  await attempt.populate(POPULATE_STUDENT);
  res.json({ attempt });
});

/** Autosaves in-progress answers - called as the student answers each question, not just at
 * final submit. This is both the "don't lose my answers" persistence the spec asks for and
 * the only way `attempt.answers` ever changes before submit (submitAttempt below trusts
 * exactly this, never a client-supplied answers body). */
const saveProgress = asyncHandler(async (req, res) => {
  const { answers } = req.body;
  const attempt = await QuizAttempt.findById(req.params.id);
  if (!attempt) throw new ApiError(404, 'Attempt not found');
  if (attempt.studentId !== req.user._id.toString()) {
    throw new ApiError(403, 'You can only update your own attempt');
  }
  if (attempt.submittedAt) {
    throw new ApiError(409, 'This quiz has already been submitted');
  }

  const quiz = await Quiz.findById(attempt.quizId);
  if (!quiz) throw new ApiError(404, 'Quiz not found');
  if (Date.now() > deadlineMillis(quiz, attempt)) {
    throw new ApiError(409, 'Time is up - this quiz can no longer be edited');
  }
  if (!Array.isArray(answers) || answers.length !== quiz.questions.length) {
    throw new ApiError(400, `answers must be an array of length ${quiz.questions.length}`);
  }

  attempt.answers = answers.map((a) => ({
    optionIndex: a && typeof a.optionIndex === 'number' ? a.optionIndex : null,
    textAnswer: a && typeof a.textAnswer === 'string' ? a.textAnswer : null,
  }));
  await attempt.save();
  await attempt.populate(POPULATE_STUDENT);
  res.json({ attempt });
});

/** The score is computed HERE from the stored answer key, never from a client-supplied
 * value - closes the auto-grading trust gap the pre-migration (on-device) grading had.
 * Finalizes the attempt started by startAttempt using whatever was last persisted via
 * saveProgress - deliberately takes no request body, so there's no way to submit a
 * different answer set than what was actually autosaved within the time limit. Only MCQ
 * questions contribute to the auto-score; TEXT questions need gradeAttempt afterwards. */
const submitAttempt = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.STUDENT) {
    throw new ApiError(403, 'Only a Student can attempt a quiz');
  }
  const quiz = await Quiz.findById(req.params.id);
  if (!quiz) throw new ApiError(404, 'Quiz not found');

  const attempt = await QuizAttempt.findOne({ quizId: quiz._id, studentId: req.user._id.toString() });
  if (!attempt) throw new ApiError(400, 'Start the quiz before submitting');
  if (attempt.submittedAt) throw new ApiError(409, 'You have already attempted this quiz');

  let score = 0;
  quiz.questions.forEach((question, i) => {
    const answer = attempt.answers[i];
    if (question.questionType === 'MCQ' && answer && typeof answer.optionIndex === 'number'
      && answer.optionIndex === question.correctOptionIndex) {
      score += question.marks;
    }
  });

  attempt.score = score;
  attempt.submittedAt = new Date();
  await attempt.save();
  await attempt.populate(POPULATE_STUDENT);

  res.status(201).json({ attempt });

  notifyAfterResponse(async () => {
    const subject = await Subject.findById(quiz.subjectId);
    if (!subject || !subject.teacherId) return;
    const hasTextQuestions = quiz.questions.some((q) => q.questionType === 'TEXT');
    await notifyUsers([subject.teacherId], {
      type: NotificationType.QUIZ_SUBMITTED,
      title: `${attempt.studentId.fullName} attempted: ${quiz.title}`,
      body: hasTextQuestions ? 'Awaiting manual grading' : `Score: ${score}/${quiz.totalMarks}`,
      actorId: req.user._id.toString(),
      relatedType: 'quizAttempt',
      relatedId: attempt._id.toString(),
    });
  });
});

/** Manual marks override - needed whenever a quiz has TEXT (written) questions, since those
 * have no answer key to auto-score against; also kept available for legacy EXAM-type
 * quizzes (see the `type` field's comment in models/Quiz.js) for backward compatibility,
 * though nothing can create new EXAM documents any more. Pure-MCQ quizzes never need this -
 * their auto-score is final. */
const gradeAttempt = asyncHandler(async (req, res) => {
  const { manualScore, feedback } = req.body;
  const attempt = await QuizAttempt.findById(req.params.id);
  if (!attempt) throw new ApiError(404, 'Attempt not found');

  const quiz = await Quiz.findById(attempt.quizId);
  const needsManualGrading = quiz && (quiz.type === 'EXAM' || quiz.questions.some((q) => q.questionType === 'TEXT'));
  if (!quiz || !needsManualGrading) {
    throw new ApiError(400, 'Manual grading is only available for quizzes with a written question (or legacy exams)');
  }
  if (typeof manualScore !== 'number' || manualScore < 0 || manualScore > attempt.totalMarks) {
    throw new ApiError(400, `manualScore must be a number between 0 and ${attempt.totalMarks}`);
  }

  const subject = await Subject.findById(attempt.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can grade this attempt');
  }

  attempt.manualScore = manualScore;
  attempt.feedback = feedback || null;
  attempt.gradedBy = req.user._id.toString();
  attempt.gradedAt = new Date();
  await attempt.save();
  await attempt.populate(POPULATE_STUDENT);

  res.json({ attempt });

  notifyAfterResponse(async () => {
    await notifyUsers([attempt.studentId._id.toString()], {
      type: NotificationType.QUIZ_GRADED,
      title: `Quiz graded: ${quiz.title}`,
      body: `Score: ${manualScore}/${attempt.totalMarks}`,
      actorId: req.user._id.toString(),
      relatedType: 'quizAttempt',
      relatedId: attempt._id.toString(),
    });
  });
});

const listAttempts = asyncHandler(async (req, res) => {
  const { quizId, subjectId } = req.query;
  const filter = {};
  if (req.user.role === UserRole.STUDENT) {
    // A student's own in-progress attempt must still be visible here - it's how
    // TakeQuizViewModel.load() resumes a quiz that was closed before submitting.
    filter.studentId = req.user._id.toString();
    if (quizId) filter.quizId = quizId;
  } else {
    if (!quizId && !subjectId) throw new ApiError(400, 'quizId or subjectId is required');
    if (quizId) filter.quizId = quizId;
    if (subjectId) filter.subjectId = subjectId;
    // Teacher/Admin/HOD attempt lists are for reviewing finished work - a student still
    // mid-quiz isn't an attempt to review yet, just noise here.
    filter.submittedAt = { $ne: null };
  }
  const attempts = await QuizAttempt.find(filter).sort({ submittedAt: -1 }).populate(POPULATE_STUDENT);
  res.json({ attempts });
});

module.exports = { create, list, getById, remove, startAttempt, saveProgress, submitAttempt, gradeAttempt, listAttempts };
