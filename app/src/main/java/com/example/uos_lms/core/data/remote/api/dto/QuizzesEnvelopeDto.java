package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuizzesEnvelopeDto {
    private List<QuizResponseDto> quizzes;
}
