package com.example.uos_lms.feature.student.presentation.quiz;

import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TakeQuizUiState {
    private final Quiz quiz;
    /** Null = never started; startedAt set but not submitted = in progress; submitted = done. */
    private final QuizAttempt existingAttempt;
    @Builder.Default
    private final List<QuizAnswer> answers = Collections.emptyList();
    @Builder.Default
    private final int secondsRemaining = 0;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean starting = false;
    @Builder.Default
    private final boolean submitting = false;
    private final String errorMessage;

    public static TakeQuizUiState initial() {
        return TakeQuizUiState.builder().build();
    }

    public boolean isInProgress() {
        return existingAttempt != null && !existingAttempt.isSubmitted();
    }
}
