package com.example.uos_lms.feature.student.presentation.quiz;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentSubjectQuizzesUiState {
    @Builder.Default
    private final List<QuizListItem> items = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static StudentSubjectQuizzesUiState initial() {
        return StudentSubjectQuizzesUiState.builder().build();
    }
}
