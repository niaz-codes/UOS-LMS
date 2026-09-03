package com.example.uos_lms.feature.teacher.presentation.leaveapplication;

import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherLeaveApplicationUiState {
    @Builder.Default
    private final List<TeacherLeaveApplication> leaves = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean applying = false;
    private final String errorMessage;
    private final String actionMessage;

    public static TeacherLeaveApplicationUiState initial() {
        return TeacherLeaveApplicationUiState.builder().build();
    }
}
