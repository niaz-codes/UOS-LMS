package com.example.uos_lms.feature.student.presentation.attendance;

import com.example.uos_lms.core.domain.model.AttendanceRecord;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentSubjectAttendanceUiState {
    @Builder.Default
    private final List<AttendanceRecord> records = Collections.emptyList();
    @Builder.Default
    private final int presentCount = 0;
    @Builder.Default
    private final int totalCount = 0;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public int getPercentage() {
        return totalCount == 0 ? 0 : (presentCount * 100) / totalCount;
    }

    public static StudentSubjectAttendanceUiState initial() {
        return StudentSubjectAttendanceUiState.builder().build();
    }
}
