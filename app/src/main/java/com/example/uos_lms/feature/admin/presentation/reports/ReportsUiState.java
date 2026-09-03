package com.example.uos_lms.feature.admin.presentation.reports;

import com.example.uos_lms.core.ui.ChartEntry;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportsUiState {
    private final int totalStudents;
    private final int totalTeachers;
    private final int totalHods;
    private final int totalDepartments;
    private final int totalSubjects;
    private final int totalAssignments;
    private final int totalQuizzes;
    @Builder.Default
    private final List<ChartEntry> departmentAttendance = Collections.emptyList();
    @Builder.Default
    private final List<ChartEntry> teacherLoad = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;

    public static ReportsUiState initial() {
        return ReportsUiState.builder().build();
    }
}
