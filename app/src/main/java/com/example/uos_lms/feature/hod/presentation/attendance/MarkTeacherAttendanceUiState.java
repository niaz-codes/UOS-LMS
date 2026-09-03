package com.example.uos_lms.feature.hod.presentation.attendance;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class MarkTeacherAttendanceUiState {
    @Builder.Default
    private final String dateLabel = "";
    @Builder.Default
    private final String semesterLabel = "";
    @Builder.Default
    private final List<SubjectAttendanceRow> rows = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean saving = false;
    private final String errorMessage;

    public static MarkTeacherAttendanceUiState initial() {
        return MarkTeacherAttendanceUiState.builder().build();
    }
}
