package com.example.uos_lms.feature.student.presentation.submissions;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentSubmissionsUiState {
    @Builder.Default
    private final List<SubmissionRow> rows = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static StudentSubmissionsUiState initial() {
        return StudentSubmissionsUiState.builder().build();
    }
}
