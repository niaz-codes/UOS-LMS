package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.domain.model.QuizAnswer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the backend's quizAnswerSchema - used both to send saveProgress requests and to
 * parse the `answers` array inside {@link QuizAttemptResponseDto}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QuizAnswerDto {
    private Integer optionIndex;
    private String textAnswer;

    public static QuizAnswerDto from(QuizAnswer answer) {
        if (answer == null) return QuizAnswerDto.builder().build();
        return QuizAnswerDto.builder().optionIndex(answer.getOptionIndex()).textAnswer(answer.getTextAnswer()).build();
    }

    public QuizAnswer toDomain() {
        return QuizAnswer.builder().optionIndex(optionIndex).textAnswer(textAnswer).build();
    }
}
