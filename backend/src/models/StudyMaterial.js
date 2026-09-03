const mongoose = require('mongoose');
const { Schema } = mongoose;

const studyMaterialSchema = new Schema(
  {
    subjectId: { type: Schema.Types.ObjectId, ref: 'Subject', required: true },
    departmentId: { type: Schema.Types.ObjectId, ref: 'Department', required: true },
    semesterId: { type: Schema.Types.ObjectId, ref: 'Semester', required: true },
    title: { type: String, required: true, trim: true },
    description: { type: String, default: '' },
    // PPT/NOTE are legacy values kept only so any pre-existing documents still deserialize
    // cleanly - new uploads only ever use PDF/VIDEO/DOCUMENT/IMAGE/OTHER (see
    // materialController.MATERIAL_TYPES).
    materialType: { type: String, enum: ['PDF', 'PPT', 'VIDEO', 'NOTE', 'DOCUMENT', 'IMAGE', 'OTHER'], default: 'OTHER' },

    mediaId: { type: Schema.Types.ObjectId, ref: 'Media', required: true },
    fileUrl: { type: String, required: true },
    fileName: { type: String, default: null },
    filePublicId: { type: String, default: null },
    fileResourceType: { type: String, default: null },
    fileSize: { type: Number, default: null },

    uploadedBy: { type: String, ref: 'User', required: true },
  },
  { timestamps: true }
);

module.exports = mongoose.model('StudyMaterial', studyMaterialSchema);
