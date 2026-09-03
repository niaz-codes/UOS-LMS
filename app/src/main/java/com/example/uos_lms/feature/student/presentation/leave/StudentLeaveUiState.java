package com.example.uos_lms.feature.student.presentation.leave;

import com.example.uos_lms.core.domain.model.LeaveApplication;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentLeaveUiState {
    @Builder.Default
    private final List<LeaveApplication> leaves = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean applying = false;
    private final String errorMessage;
    private final String actionMessage;

    public static StudentLeaveUiState initial() {
        return StudentLeaveUiState.builder().build();
    }
}
