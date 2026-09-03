package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Assignment {
    private final String id;
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String title;
    private final String description;
    private final long dueDateMillis;
    private final int maxMarks;
    private final String createdBy;
    @Builder.Default
    private final long createdAt = 0L;
    private final String fileUrl;
    private final String fileName;
    private final String filePublicId;
    private final String fileResourceType;
    private final Long fileSize;
}
