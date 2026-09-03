package com.example.uos_lms.feature.hod.presentation.dashboard;

import com.example.uos_lms.core.domain.model.Semester;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodDashboardUiState {
    @Builder.Default
    private final String fullName = "";
    private final String departmentId;
    @Builder.Default
    private final String departmentName = "";
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    @Builder.Default
    private final int teacherCount = 0;
    @Builder.Default
    private final int studentCount = 0;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static HodDashboardUiState initial() {
        return HodDashboardUiState.builder().build();
    }
}
