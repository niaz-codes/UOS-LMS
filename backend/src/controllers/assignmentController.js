const Assignment = require('../models/Assignment');
const AssignmentSubmission = require('../models/AssignmentSubmission');
const Subject = require('../models/Subject');
const Media = require('../models/Media');
const cloudinary = require('../config/cloudinary');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isHodOfDepartment } = require('../services/authorization');
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

async function resolveMediaFields(mediaId) {
  if (!mediaId) return {};
  const media = await Media.findById(mediaId);
  if (!media) return {};
  return {
    mediaId: media._id,
    fileUrl: media.secureUrl,
    fileName: media.fileName,
    filePublicId: media.publicId,
    fileResourceType: media.resourceType,
    fileSize: media.fileSize,
  };
}

const createAssignment = asyncHandler(async (req, res) => {
  const { subjectId, title, description, dueDate, maxMarks, mediaId } = req.body;
  if (!subjectId || !title || !dueDate || !maxMarks) {
    throw new ApiError(400, 'subjectId, title, dueDate, and maxMarks are required');
  }
  const subject = await requireOwnSubject(req, subjectId);

  const assignment = await Assignment.create({
    subjectId,
    departmentId: subject.departmentId,
    semesterId: subject.semesterId,
    title,
    description: description || '',
    dueDate,
    maxMarks,
    createdBy: req.user._id.toString(),
    ...(await resolveMediaFields(mediaId)),
  });
  res.status(201).json({ assignment });

  notifyAfterResponse(async () => {
    const recipientIds = await resolveStudentsInCohort(subject.departmentId, subject.semesterId);
    await notifyUsers(recipientIds, {
      type: NotificationType.ASSIGNMENT_CREATED,
      title: `New assignment: ${assignment.title}`,
      body: `${subject.title || subject.code || ''} - due ${new Date(assignment.dueDate).toDateString()}`,
      actorId: req.user._id.toString(),
      relatedType: 'assignment',
      relatedId: assignment._id.toString(),
    });
  });
});

/** Any approved user can list within a subject; HOD is forced to their own department for
 * a department-wide monitor view; Admin can pass departmentId for the same. */
const listAssignments = asyncHandler(async (req, res) => {
  const { subjectId, departmentId } = req.query;
  const filter = {};
  if (subjectId) filter.subjectId = subjectId;
  if (req.user.role === UserRole.HOD) {
    filter.departmentId = req.user.departmentId;
  } else if (departmentId) {
    filter.departmentId = departmentId;
  }
  const assignments = await Assignment.find(filter).sort({ dueDate: -1 });
  res.json({ assignments });
});

/** Admin-only, matching the pre-migration app (no delete path for Teachers, even on their
 * own assignments). Cascades to the Media asset if one was attached. */
const removeAssignment = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.ADMIN) {
    throw new ApiError(403, 'Only an Admin can delete an assignment');
  }
  const assignment = await Assignment.findById(req.params.id);
  if (!assignment) throw new ApiError(404, 'Assignment not found');

  if (assignment.mediaId) {
    const media = await Media.findById(assignment.mediaId);
    if (media) {
      await cloudinary.uploader.destroy(media.publicId, { resource_type: media.resourceType }).catch(() => {});
      await media.deleteOne();
    }
  }
  await assignment.deleteOne();
  res.status(204).send();
});

/** Student submit/resubmit - upserted by (assignmentId, studentId). Resubmitting resets
 * grading fields (marksObtained/feedback/gradedBy/gradedAt to null), matching the
 * pre-migration app's exact (if slightly surprising) behavior. If no new file is attached,
 * the previous submission's file carries forward. */
const submit = asyncHandler(async (req, res) => {
  if (req.user.role !== UserRole.STUDENT) {
    throw new ApiError(403, 'Only a Student can submit an assignment');
  }
  const { textAnswer, mediaId } = req.body;
  const assignment = await Assignment.findById(req.params.assignmentId);
  if (!assignment) throw new ApiError(404, 'Assignment not found');

  const fields = {
    assignmentId: assignment._id,
    subjectId: assignment.subjectId,
    departmentId: assignment.departmentId,
    studentId: req.user._id.toString(),
    textAnswer: textAnswer || null,
    submittedAt: new Date(),
    marksObtained: null,
    feedback: null,
    gradedBy: null,
    gradedAt: null,
    mediaId: null,
    fileUrl: null,
    fileName: null,
    filePublicId: null,
    fileResourceType: null,
    fileSize: null,
  };

  if (mediaId) {
    Object.assign(fields, await resolveMediaFields(mediaId));
  } else {
    const existing = await AssignmentSubmission.findOne({ assignmentId: assignment._id, studentId: req.user._id.toString() });
    if (existing && existing.mediaId) {
      fields.mediaId = existing.mediaId;
      fields.fileUrl = existing.fileUrl;
      fields.fileName = existing.fileName;
      fields.filePublicId = existing.filePublicId;
      fields.fileResourceType = existing.fileResourceType;
      fields.fileSize = existing.fileSize;
    }
  }

  const submission = await AssignmentSubmission.findOneAndUpdate(
    { assignmentId: assignment._id, studentId: req.user._id.toString() },
    fields,
    { new: true, upsert: true, setDefaultsOnInsert: true }
  ).populate(POPULATE_STUDENT);
  res.json({ submission });

  notifyAfterResponse(async () => {
    const subject = await Subject.findById(assignment.subjectId);
    if (!subject || !subject.teacherId) return;
    await notifyUsers([subject.teacherId], {
      type: NotificationType.ASSIGNMENT_SUBMITTED,
      title: `${submission.studentId.fullName} submitted: ${assignment.title}`,
      body: subject.title || subject.code || '',
      actorId: req.user._id.toString(),
      relatedType: 'assignmentSubmission',
      relatedId: submission._id.toString(),
    });
  });
});

/** Teacher (own subject) or HOD (own department) or Admin can grade. */
const grade = asyncHandler(async (req, res) => {
  const { marksObtained, feedback } = req.body;
  const submission = await AssignmentSubmission.findById(req.params.id);
  if (!submission) throw new ApiError(404, 'Submission not found');

  const assignment = await Assignment.findById(submission.assignmentId);
  if (typeof marksObtained !== 'number' || marksObtained < 0 || (assignment && marksObtained > assignment.maxMarks)) {
    throw new ApiError(400, `marksObtained must be a number between 0 and ${assignment ? assignment.maxMarks : 100}`);
  }

  const subject = await Subject.findById(submission.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher && !isHodOfDepartment(req.user, submission.departmentId)) {
    throw new ApiError(403, 'Not authorized to grade this submission');
  }

  submission.marksObtained = marksObtained;
  submission.feedback = feedback || null;
  submission.gradedBy = req.user._id.toString();
  submission.gradedAt = new Date();
  await submission.save();
  await submission.populate(POPULATE_STUDENT);

  res.json({ submission });

  notifyAfterResponse(async () => {
    await notifyUsers([submission.studentId._id.toString()], {
      type: NotificationType.ASSIGNMENT_GRADED,
      title: `Assignment graded: ${assignment ? assignment.title : ''}`,
      body: `Marks: ${marksObtained}${assignment ? '/' + assignment.maxMarks : ''}`,
      actorId: req.user._id.toString(),
      relatedType: 'assignmentSubmission',
      relatedId: submission._id.toString(),
    });
  });
});

const listSubmissions = asyncHandler(async (req, res) => {
  const { assignmentId, subjectId } = req.query;
  const filter = {};

  if (req.user.role === UserRole.STUDENT) {
    filter.studentId = req.user._id.toString();
    if (assignmentId) filter.assignmentId = assignmentId;
  } else {
    if (!assignmentId && !subjectId) {
      throw new ApiError(400, 'assignmentId or subjectId is required');
    }
    if (assignmentId) filter.assignmentId = assignmentId;
    if (subjectId) filter.subjectId = subjectId;
    if (req.user.role === UserRole.HOD) filter.departmentId = req.user.departmentId;
  }

  const submissions = await AssignmentSubmission.find(filter).sort({ submittedAt: -1 }).populate(POPULATE_STUDENT);
  res.json({ submissions });
});

module.exports = { createAssignment, listAssignments, removeAssignment, submit, grade, listSubmissions };
