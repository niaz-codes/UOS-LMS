package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminStudentTreeUiState {
    @Builder.Default
    private final String departmentSearchQuery = "";
    @Builder.Default
    private final List<DepartmentStudentNode> nodes = Collections.emptyList();
    @Builder.Default
    private final boolean loadingDepartments = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final Integer totalStudents;
    private final Integer totalSemesters;
    private final Integer totalSessions;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminStudentTreeUiState initial() {
        return AdminStudentTreeUiState.builder().build();
    }

    public List<DepartmentStudentNode> getFilteredNodes() {
        if (departmentSearchQuery.isBlank()) return nodes;
        String query = departmentSearchQuery.toLowerCase();
        List<DepartmentStudentNode> result = new ArrayList<>();
        for (DepartmentStudentNode node : nodes) {
            if (node.getDepartment().getName().toLowerCase().contains(query)
                    || node.getDepartment().getCode().toLowerCase().contains(query)) {
                result.add(node);
            }
        }
        return result;
    }
}
