package com.example.uos_lms.feature.admin.university.presentation.department;

import com.example.uos_lms.core.domain.model.Department;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class DepartmentListUiState {
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String listErrorMessage;

    public static DepartmentListUiState initial() {
        return DepartmentListUiState.builder().build();
    }
}
