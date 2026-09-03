const { Readable } = require('stream');
const cloudinary = require('../config/cloudinary');
const Media = require('../models/Media');
const { ApiError } = require('../middleware/errorHandler');
const { MediaCategory } = require('../constants/enums');

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const ALLOWED_IMAGE_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function validateImageFile(file) {
  if (!file) {
    throw new ApiError(400, 'file is required');
  }
  if (!ALLOWED_IMAGE_MIME_TYPES.has(file.mimetype)) {
    throw new ApiError(400, 'Only JPG, PNG, or WEBP images are allowed.');
  }
  if (file.size > MAX_IMAGE_BYTES) {
    throw new ApiError(400, 'Image must be smaller than 5 MB.');
  }
}

function resolveResourceType(mimetype) {
  if (!mimetype) return 'raw';
  if (mimetype.startsWith('image/')) return 'image';
  if (mimetype.startsWith('video/')) return 'video';
  return 'raw';
}

function bufferToStream(buffer) {
  // Readable.from(buffer) would iterate the Buffer byte-by-byte (Buffer is iterable via
  // Uint8Array's Symbol.iterator, yielding individual numbers) instead of emitting it as
  // one chunk - push the whole buffer as a single chunk instead.
  const readable = new Readable();
  readable._read = () => {};
  readable.push(buffer);
  readable.push(null);
  return readable;
}

function uploadBufferToCloudinary(buffer, resourceType, { folder, publicId }) {
  return new Promise((resolve, reject) => {
    const options = { resource_type: resourceType, overwrite: true, invalidate: true };
    if (publicId) {
      options.public_id = publicId;
    } else {
      options.folder = folder;
    }
    const uploadStream = cloudinary.uploader.upload_stream(options, (error, result) => {
      if (error) return reject(error);
      resolve(result);
    });
    bufferToStream(buffer).pipe(uploadStream);
  });
}

/** Uploads `file` to Cloudinary as `userId`'s profile photo (deterministic public_id, one
 * asset per user, upload doubles as replace) and upserts the matching Media record, so this
 * stays the single source of truth regardless of whether the photo was set at registration
 * (authController.register) or later from the Profile screen (mediaController.upload). */
async function saveProfilePhoto(userId, file) {
  validateImageFile(file);
  const resourceType = resolveResourceType(file.mimetype);
  const result = await uploadBufferToCloudinary(file.buffer, resourceType, {
    publicId: `profile_photo/${userId}`,
  });

  const media = await Media.findOneAndUpdate(
    { category: MediaCategory.PROFILE_PHOTO, uploadedBy: userId },
    {
      secureUrl: result.secure_url,
      publicId: result.public_id,
      resourceType: result.resource_type,
      fileName: file.originalname,
      fileType: file.mimetype,
      fileSize: file.size,
      category: MediaCategory.PROFILE_PHOTO,
      uploadedBy: userId,
      relatedTo: { type: null, id: userId },
    },
    { new: true, upsert: true }
  );

  return media;
}

module.exports = {
  MAX_IMAGE_BYTES,
  ALLOWED_IMAGE_MIME_TYPES,
  validateImageFile,
  resolveResourceType,
  bufferToStream,
  uploadBufferToCloudinary,
  saveProfilePhoto,
};
