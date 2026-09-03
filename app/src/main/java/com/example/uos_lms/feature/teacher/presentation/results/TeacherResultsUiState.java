package com.example.uos_lms.feature.teacher.presentation.results;

import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherResultsUiState {
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static TeacherResultsUiState initial() {
        return TeacherResultsUiState.builder().build();
    }
}
