package com.example.uos_lms.feature.teacher.presentation.assignment;

import com.example.uos_lms.core.domain.model.AssignmentSubmission;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AssignmentSubmissionsUiState {
    @Builder.Default
    private final List<AssignmentSubmission> submissions = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static AssignmentSubmissionsUiState initial() {
        return AssignmentSubmissionsUiState.builder().build();
    }
}
