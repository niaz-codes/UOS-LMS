package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateQuizRequestDto {
    private final String subjectId;
    private final String title;
    private final String description;
    private final List<QuizQuestionRequestDto> questions;
    private final int timeLimitMinutes;
    private final long dueDate;
}
