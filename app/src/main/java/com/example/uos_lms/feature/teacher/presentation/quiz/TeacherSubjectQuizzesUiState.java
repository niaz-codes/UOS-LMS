package com.example.uos_lms.feature.teacher.presentation.quiz;

import com.example.uos_lms.core.domain.model.Quiz;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherSubjectQuizzesUiState {
    @Builder.Default
    private final List<Quiz> quizzes = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static TeacherSubjectQuizzesUiState initial() {
        return TeacherSubjectQuizzesUiState.builder().build();
    }
}
