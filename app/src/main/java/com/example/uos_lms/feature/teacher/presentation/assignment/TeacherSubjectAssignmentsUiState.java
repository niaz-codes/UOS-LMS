package com.example.uos_lms.feature.teacher.presentation.assignment;

import com.example.uos_lms.core.domain.model.Assignment;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherSubjectAssignmentsUiState {
    @Builder.Default
    private final List<Assignment> assignments = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static TeacherSubjectAssignmentsUiState initial() {
        return TeacherSubjectAssignmentsUiState.builder().build();
    }
}
