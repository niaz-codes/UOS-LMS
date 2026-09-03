package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Assignment;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssignmentResponseDto {
    @SerializedName("_id")
    private String id;
    private String subjectId;
    private String departmentId;
    private String semesterId;
    private String title;
    private String description;
    private String dueDate;
    private int maxMarks;
    private String createdBy;
    private String createdAt;
    private String fileUrl;
    private String fileName;
    private String filePublicId;
    private String fileResourceType;
    private Long fileSize;

    public Assignment toDomain() {
        return Assignment.builder()
                .id(id)
                .subjectId(subjectId)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .title(title)
                .description(description)
                .dueDateMillis(IsoDates.toMillis(dueDate))
                .maxMarks(maxMarks)
                .createdBy(createdBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .fileUrl(fileUrl)
                .fileName(fileName)
                .filePublicId(filePublicId)
                .fileResourceType(fileResourceType)
                .fileSize(fileSize)
                .build();
    }
}
