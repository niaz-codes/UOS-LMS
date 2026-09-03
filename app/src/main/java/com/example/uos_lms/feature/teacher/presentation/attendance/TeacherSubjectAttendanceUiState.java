package com.example.uos_lms.feature.teacher.presentation.attendance;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class TeacherSubjectAttendanceUiState {
    @Builder.Default
    private final List<String> historyDates = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static TeacherSubjectAttendanceUiState initial() {
        return TeacherSubjectAttendanceUiState.builder().build();
    }
}
