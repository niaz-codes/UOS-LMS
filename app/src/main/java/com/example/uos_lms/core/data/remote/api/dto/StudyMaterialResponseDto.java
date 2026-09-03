package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.MaterialType;
import com.example.uos_lms.core.domain.model.StudyMaterial;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudyMaterialResponseDto {
    @SerializedName("_id")
    private String id;
    private String subjectId;
    private String departmentId;
    private String semesterId;
    private String title;
    private String description;
    private String materialType;
    private String fileUrl;
    private String fileName;
    private String filePublicId;
    private String fileResourceType;
    private Long fileSize;
    private PersonRefDto uploadedBy;
    private String createdAt;

    public StudyMaterial toDomain() {
        return StudyMaterial.builder()
                .id(id)
                .subjectId(subjectId)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .title(title)
                .description(description != null ? description : "")
                .materialType(MaterialType.fromStringOrNull(materialType))
                .fileUrl(fileUrl)
                .fileName(fileName)
                .filePublicId(filePublicId)
                .fileResourceType(fileResourceType)
                .fileSize(fileSize != null ? fileSize : 0L)
                .uploadedBy(uploadedBy != null ? uploadedBy.getId() : null)
                .uploadedByName(uploadedBy != null ? uploadedBy.getFullName() : null)
                .uploadedAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
