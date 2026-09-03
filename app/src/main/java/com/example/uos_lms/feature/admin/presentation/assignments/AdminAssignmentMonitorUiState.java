package com.example.uos_lms.feature.admin.presentation.assignments;

import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminAssignmentMonitorUiState {
    @Builder.Default
    private final List<Assignment> assignments = Collections.emptyList();
    @Builder.Default
    private final Map<String, Subject> subjectsById = Collections.emptyMap();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static AdminAssignmentMonitorUiState initial() {
        return AdminAssignmentMonitorUiState.builder().build();
    }
}
