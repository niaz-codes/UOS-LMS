package com.example.uos_lms.feature.teacher.presentation.attendance;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class MarkAttendanceUiState {
    @Builder.Default
    private final String dateLabel = "";
    @Builder.Default
    private final List<RosterRow> rows = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean saving = false;
    private final String errorMessage;

    public static MarkAttendanceUiState initial() {
        return MarkAttendanceUiState.builder().build();
    }
}
