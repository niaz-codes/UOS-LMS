package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizQuestionRequestDto {
    private final String text;
    private final String questionType;
    private final List<String> options;
    private final int correctOptionIndex;
    private final int marks;
}
