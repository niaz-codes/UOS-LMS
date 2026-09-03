package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeQuizAttemptRequestDto {
    private final int manualScore;
    private final String feedback;
}
