package com.example.uos_lms.feature.teacher.presentation.dashboard;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherDashboardUiState {
    @Builder.Default
    private final String fullName = "";
    @Builder.Default
    private final List<AssignedSubject> subjects = Collections.emptyList();
    @Builder.Default
    private final int totalStudents = 0;
    @Builder.Default
    private final int totalSubmissions = 0;
    @Builder.Default
    private final int attendancePercentage = 0;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    private final String actionMessage;

    public static TeacherDashboardUiState initial() {
        return TeacherDashboardUiState.builder().build();
    }
}
