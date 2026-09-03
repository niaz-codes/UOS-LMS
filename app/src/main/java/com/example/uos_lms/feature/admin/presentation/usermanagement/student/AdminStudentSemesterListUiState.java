package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminStudentSemesterListUiState {
    @Builder.Default
    private final String departmentName = "";
    @Builder.Default
    private final String sessionLabel = "";
    @Builder.Default
    private final String semesterSearchQuery = "";
    @Builder.Default
    private final List<SemesterStudentsNode> semesters = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminStudentSemesterListUiState initial() {
        return AdminStudentSemesterListUiState.builder().build();
    }

    public List<SemesterStudentsNode> getFilteredSemesters() {
        if (semesterSearchQuery.isBlank()) return semesters;
        String query = semesterSearchQuery.toLowerCase();
        List<SemesterStudentsNode> result = new ArrayList<>();
        for (SemesterStudentsNode node : semesters) {
            if (node.getSemester().getDisplayName().toLowerCase().contains(query)) result.add(node);
        }
        return result;
    }
}
