package com.example.uos_lms.feature.hod.presentation.reports;

import com.example.uos_lms.core.ui.ChartEntry;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodReportsUiState {
    @Builder.Default
    private final int totalTeachers = 0;
    @Builder.Default
    private final int totalStudents = 0;
    @Builder.Default
    private final int totalSubjects = 0;
    @Builder.Default
    private final int totalSemesters = 0;
    @Builder.Default
    private final List<ChartEntry> semesterAttendance = Collections.emptyList();
    @Builder.Default
    private final List<ChartEntry> teacherLoad = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static HodReportsUiState initial() {
        return HodReportsUiState.builder().build();
    }
}
