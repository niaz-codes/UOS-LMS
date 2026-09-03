package com.example.uos_lms.feature.student.presentation.attendance;

import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.example.uos_lms.core.ui.ChartEntry;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentAttendanceUiState {
    @Builder.Default
    private final int overallPercentage = 0;
    @Builder.Default
    private final int presentCount = 0;
    @Builder.Default
    private final int totalCount = 0;
    @Builder.Default
    private final List<ChartEntry> bySubject = Collections.emptyList();
    @Builder.Default
    private final List<LeaveApplication> approvedLeaves = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static StudentAttendanceUiState initial() {
        return StudentAttendanceUiState.builder().build();
    }
}
