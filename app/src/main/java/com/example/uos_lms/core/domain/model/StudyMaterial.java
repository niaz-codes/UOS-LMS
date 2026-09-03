package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class StudyMaterial {
    private final String id;
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String title;
    @Builder.Default
    private final String description = "";
    private final MaterialType materialType;
    private final String fileUrl;
    private final String fileName;
    private final String filePublicId;
    private final String fileResourceType;
    @Builder.Default
    private final long fileSize = 0L;
    private final String uploadedBy;
    private final String uploadedByName;
    @Builder.Default
    private final long uploadedAt = 0L;
}
