const cloudinary = require('../config/cloudinary');
const Media = require('../models/Media');
const { ApiError } = require('../middleware/errorHandler');
const { asyncHandler } = require('../utils/asyncHandler');
const { MediaCategory, UserRole } = require('../constants/enums');
const {
  validateImageFile,
  resolveResourceType,
  uploadBufferToCloudinary,
  saveProfilePhoto,
} = require('../services/cloudinaryUpload');

const MAX_DOCUMENT_BYTES = 25 * 1024 * 1024;

const ALLOWED_DOCUMENT_MIME_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain',
  'application/zip',
  'video/mp4',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

// Only profile_photo is image-only (matches the app's existing 5MB/JPEG-PNG-WEBP-only
// profile photo rule) - every other category accepts the broader document allow-list.
function validateFile(category, file) {
  if (!file) {
    throw new ApiError(400, 'file is required');
  }
  if (category === MediaCategory.PROFILE_PHOTO) {
    validateImageFile(file);
    return;
  }
  if (!ALLOWED_DOCUMENT_MIME_TYPES.has(file.mimetype)) {
    throw new ApiError(400, "This file type isn't supported.");
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    throw new ApiError(400, 'File must be smaller than 25 MB.');
  }
}

function buildFolder(category, uploadedBy, relatedId) {
  return `${category}/${relatedId || uploadedBy}`;
}

function canManage(user, media) {
  return user.role === UserRole.ADMIN || media.uploadedBy === user._id.toString();
}

const upload = asyncHandler(async (req, res) => {
  const { category, relatedType, relatedId } = req.body;
  if (!Object.values(MediaCategory).includes(category)) {
    throw new ApiError(400, `category must be one of: ${Object.values(MediaCategory).join(', ')}`);
  }

  const uploadedBy = req.user._id.toString();

  // Profile photos use a deterministic public_id (one asset per user, "upload" doubles as
  // "replace") so the client never has to track a Media _id just to change their photo -
  // matches the folder convention the app used before this migration (profile_photos/{uid}).
  // Shared with authController.register, which sets the initial photo at signup time.
  if (category === MediaCategory.PROFILE_PHOTO) {
    const media = await saveProfilePhoto(uploadedBy, req.file);
    return res.status(201).json({ media });
  }

  validateFile(category, req.file);
  const resourceType = resolveResourceType(req.file.mimetype);
  const result = await uploadBufferToCloudinary(req.file.buffer, resourceType, {
    folder: buildFolder(category, uploadedBy, relatedId),
  });

  const media = await Media.create({
    secureUrl: result.secure_url,
    publicId: result.public_id,
    resourceType: result.resource_type,
    fileName: req.file.originalname,
    fileType: req.file.mimetype,
    fileSize: req.file.size,
    category,
    uploadedBy,
    relatedTo: { type: relatedType || null, id: relatedId || null },
  });

  res.status(201).json({ media });
});

const replace = asyncHandler(async (req, res) => {
  const media = await Media.findById(req.params.id);
  if (!media) throw new ApiError(404, 'Media not found');
  if (!canManage(req.user, media)) {
    throw new ApiError(403, 'Not authorized to modify this file');
  }
  validateFile(media.category, req.file);

  await cloudinary.uploader.destroy(media.publicId, { resource_type: media.resourceType }).catch(() => {
    // best-effort cleanup of the old asset - a missing/already-deleted asset isn't fatal
  });

  const resourceType = resolveResourceType(req.file.mimetype);
  const result = await uploadBufferToCloudinary(req.file.buffer, resourceType, { folder: buildFolder(media.category, media.uploadedBy, media.relatedTo?.id) });

  media.secureUrl = result.secure_url;
  media.publicId = result.public_id;
  media.resourceType = result.resource_type;
  media.fileName = req.file.originalname;
  media.fileType = req.file.mimetype;
  media.fileSize = req.file.size;
  await media.save();

  res.json({ media });
});

const remove = asyncHandler(async (req, res) => {
  const media = await Media.findById(req.params.id);
  if (!media) throw new ApiError(404, 'Media not found');
  if (!canManage(req.user, media)) {
    throw new ApiError(403, 'Not authorized to delete this file');
  }

  await cloudinary.uploader.destroy(media.publicId, { resource_type: media.resourceType }).catch(() => {
    // best-effort - proceed with removing the Media record either way
  });
  await media.deleteOne();

  res.status(204).send();
});

const getById = asyncHandler(async (req, res) => {
  const media = await Media.findById(req.params.id);
  if (!media) throw new ApiError(404, 'Media not found');
  res.json({ media });
});

/** Best-effort - a missing/already-deleted asset is not treated as an error, matching the
 * old client-side CloudinaryDataSource.deleteProfilePhoto behavior it replaces. */
const removeMyProfilePhoto = asyncHandler(async (req, res) => {
  const uploadedBy = req.user._id.toString();
  const media = await Media.findOneAndDelete({ category: MediaCategory.PROFILE_PHOTO, uploadedBy });
  if (media) {
    await cloudinary.uploader.destroy(media.publicId, { resource_type: media.resourceType }).catch(() => {});
  }
  res.status(204).send();
});

module.exports = { upload, replace, remove, getById, removeMyProfilePhoto };
