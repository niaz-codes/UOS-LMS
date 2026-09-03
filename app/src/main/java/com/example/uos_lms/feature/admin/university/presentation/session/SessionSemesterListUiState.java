package com.example.uos_lms.feature.admin.university.presentation.session;

import com.example.uos_lms.core.domain.model.Semester;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SessionSemesterListUiState {
    @Builder.Default
    private final String departmentName = "";
    @Builder.Default
    private final String sessionLabel = "";
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static SessionSemesterListUiState initial() {
        return SessionSemesterListUiState.builder().build();
    }
}
