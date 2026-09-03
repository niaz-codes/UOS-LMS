const multer = require('multer');

const MAX_DOCUMENT_BYTES = 25 * 1024 * 1024;

// Actual per-category limits (5MB for images, 25MB for documents) are enforced in
// mediaController - this is just a blanket upper bound so multer rejects grossly
// oversized bodies before they're ever buffered into memory.
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_DOCUMENT_BYTES },
});

module.exports = { upload };
