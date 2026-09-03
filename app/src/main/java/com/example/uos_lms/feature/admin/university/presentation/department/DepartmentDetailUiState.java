package com.example.uos_lms.feature.admin.university.presentation.department;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Session;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DepartmentDetailUiState {
    private final Department department;
    @Builder.Default
    private final List<Session> sessions = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    @Builder.Default
    private final Set<String> updatingSessionIds = Collections.emptySet();
    private final SessionDeleteCheck sessionDeleteCheck;
    @Builder.Default
    private final boolean checkingSessionDelete = false;

    public static DepartmentDetailUiState initial() {
        return DepartmentDetailUiState.builder().build();
    }
}
