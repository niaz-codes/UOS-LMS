package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** One student answer to one quiz question - optionIndex for MCQ, textAnswer for TEXT.
 * Parallel array to {@link Quiz#getQuestions()} on {@link QuizAttempt#getAnswers()}. */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class QuizAnswer {
    private final Integer optionIndex;
    private final String textAnswer;

    public static QuizAnswer empty() {
        return QuizAnswer.builder().build();
    }

    public boolean isEmpty() {
        return optionIndex == null && (textAnswer == null || textAnswer.isBlank());
    }
}
