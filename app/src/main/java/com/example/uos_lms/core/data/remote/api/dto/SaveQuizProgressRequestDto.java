package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveQuizProgressRequestDto {
    private final List<QuizAnswerDto> answers;
}
