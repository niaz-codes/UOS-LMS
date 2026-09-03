package com.example.uos_lms.feature.teacher.presentation.leave;

import com.example.uos_lms.core.domain.model.LeaveApplication;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherLeaveUiState {
    @Builder.Default
    private final List<LeaveApplication> pending = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String processingLeaveId;
    private final String errorMessage;
    private final String actionMessage;

    public static TeacherLeaveUiState initial() {
        return TeacherLeaveUiState.builder().build();
    }
}
