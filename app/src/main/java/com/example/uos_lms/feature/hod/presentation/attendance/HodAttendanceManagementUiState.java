package com.example.uos_lms.feature.hod.presentation.attendance;

import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodAttendanceManagementUiState {
    private final String departmentId;
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    private final Semester selectedSemester;
    @Builder.Default
    private final List<Subject> subjectsInSemester = Collections.emptyList();
    private final Subject selectedSubject;
    private final Semester selectedTeacherSemester;
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static HodAttendanceManagementUiState initial() {
        return HodAttendanceManagementUiState.builder().build();
    }
}
