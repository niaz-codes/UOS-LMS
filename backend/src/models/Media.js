const mongoose = require('mongoose');
const { MediaCategory } = require('../constants/enums');

const { Schema } = mongoose;

const mediaSchema = new Schema(
  {
    secureUrl: { type: String, required: true },
    publicId: { type: String, required: true },
    resourceType: { type: String, required: true }, // image | raw | video
    fileName: { type: String, default: null },
    fileType: { type: String, default: null },
    fileSize: { type: Number, default: null },
    category: { type: String, enum: Object.values(MediaCategory), required: true },
    uploadedBy: { type: String, ref: 'User', required: true },
    relatedTo: {
      type: { type: String, default: null },
      id: { type: String, default: null },
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Media', mediaSchema);
