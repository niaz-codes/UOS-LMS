package com.example.uos_lms.feature.hod.presentation.quizzes;

import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodQuizMonitorUiState {
    @Builder.Default
    private final List<Quiz> quizzes = Collections.emptyList();
    @Builder.Default
    private final Map<String, Subject> subjectsById = Collections.emptyMap();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static HodQuizMonitorUiState initial() {
        return HodQuizMonitorUiState.builder().build();
    }
}
