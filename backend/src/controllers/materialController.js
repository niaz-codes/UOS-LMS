const StudyMaterial = require('../models/StudyMaterial');
const Subject = require('../models/Subject');
const Media = require('../models/Media');
const cloudinary = require('../config/cloudinary');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { UserRole, NotificationType } = require('../constants/enums');
const { isStudentEnrolledInSubject } = require('../services/authorization');
const { notifyUsers, notifyAfterResponse, resolveStudentsInCohort } = require('../services/notificationService');

const POPULATE_UPLOADER = { path: 'uploadedBy', select: 'fullName' };

// New uploads only ever pick from these - PPT/NOTE stay legal on the schema (see
// models/StudyMaterial.js) purely so any pre-existing documents keep deserializing.
const MATERIAL_TYPES = ['PDF', 'VIDEO', 'DOCUMENT', 'IMAGE', 'OTHER'];

async function requireOwnSubject(req, subjectId) {
  const subject = await Subject.findById(subjectId);
  if (!subject) throw new ApiError(404, 'Subject not found');
  const isAssignedTeacher = subject.teacherId && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can do this');
  }
  return subject;
}

const create = asyncHandler(async (req, res) => {
  const { subjectId, title, description, materialType, mediaId } = req.body;
  if (!subjectId || !title || !mediaId) {
    throw new ApiError(400, 'subjectId, title, and mediaId are required');
  }

  const subject = await requireOwnSubject(req, subjectId);

  const media = await Media.findById(mediaId);
  if (!media) throw new ApiError(404, 'Uploaded file not found - upload it via /api/media/upload first');

  const material = await StudyMaterial.create({
    subjectId,
    departmentId: subject.departmentId,
    semesterId: subject.semesterId,
    title,
    description: description || '',
    materialType: MATERIAL_TYPES.includes(materialType) ? materialType : 'OTHER',
    mediaId: media._id,
    fileUrl: media.secureUrl,
    fileName: media.fileName,
    filePublicId: media.publicId,
    fileResourceType: media.resourceType,
    fileSize: media.fileSize,
    uploadedBy: req.user._id.toString(),
  });
  await material.populate(POPULATE_UPLOADER);
  res.status(201).json({ material });

  notifyAfterResponse(async () => {
    const recipientIds = await resolveStudentsInCohort(subject.departmentId, subject.semesterId);
    await notifyUsers(recipientIds, {
      type: NotificationType.MATERIAL_UPLOADED,
      title: `New material: ${material.title}`,
      body: subject.title || subject.code || '',
      actorId: req.user._id.toString(),
      relatedType: 'studyMaterial',
      relatedId: material._id.toString(),
    });
  });
});

/** A Student may only list a subject's materials if they're actually enrolled in it (current
 * cohort or an active retake) - otherwise any authenticated student could pull another
 * class's material by guessing/reusing a subjectId. Teacher/HOD/Admin are unrestricted here,
 * matching every other list endpoint in this codebase (assignments, quizzes, etc.). */
const list = asyncHandler(async (req, res) => {
  const { subjectId } = req.query;
  if (!subjectId) throw new ApiError(400, 'subjectId is required');

  if (req.user.role === UserRole.STUDENT) {
    const subject = await Subject.findById(subjectId);
    if (!subject) throw new ApiError(404, 'Subject not found');
    if (!isStudentEnrolledInSubject(req.user, subject)) {
      throw new ApiError(403, 'You are not enrolled in this subject');
    }
  }

  const materials = await StudyMaterial.find({ subjectId }).sort({ createdAt: -1 }).populate(POPULATE_UPLOADER);
  res.json({ materials });
});

/** Metadata-only - title/description/materialType. Re-uploading a replacement file is a
 * delete + fresh upload, not an edit (keeps the Cloudinary asset lifecycle simple and matches
 * how every other "edit" in this app that involves a file already works). */
const update = asyncHandler(async (req, res) => {
  const material = await StudyMaterial.findById(req.params.id);
  if (!material) throw new ApiError(404, 'Material not found');

  const subject = await Subject.findById(material.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can edit this material');
  }

  const { title, description, materialType } = req.body;
  if (title !== undefined) {
    if (!title.trim()) throw new ApiError(400, 'title cannot be blank');
    material.title = title.trim();
  }
  if (description !== undefined) material.description = description;
  if (materialType !== undefined) {
    if (!MATERIAL_TYPES.includes(materialType)) {
      throw new ApiError(400, `materialType must be one of: ${MATERIAL_TYPES.join(', ')}`);
    }
    material.materialType = materialType;
  }
  await material.save();
  await material.populate(POPULATE_UPLOADER);
  res.json({ material });
});

const remove = asyncHandler(async (req, res) => {
  const material = await StudyMaterial.findById(req.params.id);
  if (!material) throw new ApiError(404, 'Material not found');

  const subject = await Subject.findById(material.subjectId);
  const isAssignedTeacher = subject && subject.teacherId === req.user._id.toString();
  if (req.user.role !== UserRole.ADMIN && !isAssignedTeacher) {
    throw new ApiError(403, 'Only the subject\'s assigned teacher (or an Admin) can delete this material');
  }

  const media = await Media.findById(material.mediaId);
  if (media) {
    await cloudinary.uploader.destroy(media.publicId, { resource_type: media.resourceType }).catch(() => {});
    await media.deleteOne();
  }
  await material.deleteOne();
  res.status(204).send();
});

module.exports = { create, list, update, remove };
