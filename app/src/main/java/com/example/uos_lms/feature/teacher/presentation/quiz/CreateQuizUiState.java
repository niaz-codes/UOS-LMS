package com.example.uos_lms.feature.teacher.presentation.quiz;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreateQuizUiState {
    @Builder.Default
    private final String title = "";
    @Builder.Default
    private final String description = "";
    @Builder.Default
    private final String timeLimitText = "";
    private final Long dueDateMillis;
    @Builder.Default
    private final List<QuestionDraft> questions = Collections.singletonList(QuestionDraft.builder().build());
    @Builder.Default
    private final boolean saving = false;
    private final String errorMessage;

    public static CreateQuizUiState initial() {
        return CreateQuizUiState.builder().build();
    }
}
