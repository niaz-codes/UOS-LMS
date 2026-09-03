package com.example.uos_lms.feature.teacher.presentation.reports;

import com.example.uos_lms.core.ui.ChartEntry;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherReportsUiState {
    @Builder.Default
    private final int totalSubjects = 0;
    @Builder.Default
    private final int totalStudents = 0;
    @Builder.Default
    private final int totalAssignments = 0;
    @Builder.Default
    private final int totalQuizzes = 0;
    @Builder.Default
    private final List<ChartEntry> attendanceBySubject = Collections.emptyList();
    @Builder.Default
    private final List<ChartEntry> performanceBySubject = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static TeacherReportsUiState initial() {
        return TeacherReportsUiState.builder().build();
    }
}
