package com.example.uos_lms.feature.teacher.presentation.quiz;

import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.domain.model.QuizQuestion;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class QuizAttemptsUiState {
    @Builder.Default
    private final List<QuizAttempt> attempts = Collections.emptyList();
    private final Quiz quiz;
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static QuizAttemptsUiState initial() {
        return QuizAttemptsUiState.builder().build();
    }

    /** Whether any question in this quiz needs a human to read it and assign marks - a
     * written (TEXT) question has no answer key to auto-score against. Pure-MCQ quizzes never
     * need this; their auto-score is final. */
    public boolean needsManualGrading() {
        return quiz != null && quiz.getQuestions().stream().anyMatch(QuizQuestion::isText);
    }
}
