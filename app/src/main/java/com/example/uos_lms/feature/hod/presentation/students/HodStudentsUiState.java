package com.example.uos_lms.feature.hod.presentation.students;

import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodStudentsUiState {
    private final String departmentId;
    @Builder.Default
    private final List<User> allStudents = Collections.emptyList();
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    @Builder.Default
    private final String searchQuery = "";
    private final Semester selectedSemester;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;

    public static HodStudentsUiState initial() {
        return HodStudentsUiState.builder().build();
    }

    public List<User> getFilteredStudents() {
        List<User> result = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        for (User user : allStudents) {
            if (user.getRole() != UserRole.STUDENT) continue;
            if (selectedSemester != null && !selectedSemester.getId().equals(user.getSemester())) continue;
            if (!query.isEmpty()
                    && !user.getFullName().toLowerCase().contains(query)
                    && !user.getEmail().toLowerCase().contains(query)
                    && !user.getCnic().toLowerCase().contains(query)) {
                continue;
            }
            result.add(user);
        }
        result.sort((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()));
        return result;
    }
}
