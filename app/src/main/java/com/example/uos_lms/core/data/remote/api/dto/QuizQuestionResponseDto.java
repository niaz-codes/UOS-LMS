package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.domain.model.QuestionType;
import com.example.uos_lms.core.domain.model.QuizQuestion;

import lombok.Data;
import lombok.NoArgsConstructor;

/** correctOptionIndex is absent (null) whenever the backend has stripped the answer key -
 * a Student who hasn't attempted the quiz yet (see quizController.stripAnswerKey). Mapped to
 * -1 rather than defaulting to 0, so it's never mistaken for a real "option A is correct".
 * questionType defaults to MCQ when absent, tolerating any quiz docs created before Plain
 * Text questions existed. */
@Data
@NoArgsConstructor
public class QuizQuestionResponseDto {
    private String text;
    private String questionType;
    private java.util.List<String> options;
    private Integer correctOptionIndex;
    private int marks;

    public QuizQuestion toDomain() {
        QuestionType type = QuestionType.fromStringOrNull(questionType);
        return QuizQuestion.builder()
                .text(text)
                .questionType(type != null ? type : QuestionType.MCQ)
                .options(options != null ? options : java.util.Collections.emptyList())
                .correctOptionIndex(correctOptionIndex != null ? correctOptionIndex : -1)
                .marks(marks)
                .build();
    }
}
