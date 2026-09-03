package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateAssignmentRequestDto {
    private final String subjectId;
    private final String title;
    private final String description;
    private final long dueDate;
    private final int maxMarks;
    private final String mediaId;
}
