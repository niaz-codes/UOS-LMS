package com.example.uos_lms.feature.admin.presentation.usermanagement.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminRoleTreeUiState {
    @Builder.Default
    private final String departmentSearchQuery = "";
    @Builder.Default
    private final List<DepartmentUserNode> nodes = Collections.emptyList();
    @Builder.Default
    private final boolean loadingDepartments = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final Integer totalCount;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminRoleTreeUiState initial() {
        return AdminRoleTreeUiState.builder().build();
    }

    public List<DepartmentUserNode> getFilteredNodes() {
        if (departmentSearchQuery.isBlank()) return nodes;
        String query = departmentSearchQuery.toLowerCase();
        List<DepartmentUserNode> result = new ArrayList<>();
        for (DepartmentUserNode node : nodes) {
            if (node.getDepartment().getName().toLowerCase().contains(query)
                    || node.getDepartment().getCode().toLowerCase().contains(query)) {
                result.add(node);
            }
        }
        return result;
    }
}
