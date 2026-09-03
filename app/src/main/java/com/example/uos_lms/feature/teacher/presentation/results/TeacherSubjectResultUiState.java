package com.example.uos_lms.feature.teacher.presentation.results;

import com.example.uos_lms.core.domain.model.ExamResult;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherSubjectResultUiState {
    @Builder.Default
    private final List<ExamResult> results = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static TeacherSubjectResultUiState initial() {
        return TeacherSubjectResultUiState.builder().build();
    }
}
