package com.example.uos_lms.feature.student.presentation.dashboard;

import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentDashboardUiState {
    @Builder.Default
    private final String fullName = "";
    private final String departmentId;
    @Builder.Default
    private final String departmentName = "";
    private final String semesterId;
    @Builder.Default
    private final String semesterLabel = "";
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final int attendancePercentage = 0;
    @Builder.Default
    private final int pendingAssignments = 0;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static StudentDashboardUiState initial() {
        return StudentDashboardUiState.builder().build();
    }
}
