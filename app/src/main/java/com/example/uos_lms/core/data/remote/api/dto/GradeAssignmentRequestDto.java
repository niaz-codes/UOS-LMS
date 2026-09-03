package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeAssignmentRequestDto {
    private final int marksObtained;
    private final String feedback;
}
