package com.example.uos_lms.feature.admin.university.presentation.semester;

import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.example.uos_lms.feature.admin.presentation.usermanagement.common.UserSortHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SemesterSubjectsUiState {

    public static final int PAGE_SIZE = 20;

    private final Semester semester;
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final List<User> teachers = Collections.emptyList();
    @Builder.Default
    private final String departmentName = "";
    @Builder.Default
    private final String sessionLabel = "";
    @Builder.Default
    private final List<User> students = Collections.emptyList();
    @Builder.Default
    private final boolean loadingStudents = true;
    @Builder.Default
    private final String studentSearchQuery = "";
    @Builder.Default
    private final UserFilter studentFilter = UserFilter.ALL;
    @Builder.Default
    private final UserSortOption studentSortOption = UserSortOption.NAME_ASC;
    @Builder.Default
    private final int visibleStudentCount = PAGE_SIZE;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    private final String actionMessage;

    public static SemesterSubjectsUiState initial() {
        return SemesterSubjectsUiState.builder().build();
    }

    public List<User> getFilteredStudents() {
        List<User> result = new ArrayList<>();
        String query = studentSearchQuery.toLowerCase();
        for (User user : students) {
            if (studentFilter.getStatus() != null && user.getStatus() != studentFilter.getStatus()) continue;
            if (!query.isEmpty()
                    && !user.getFullName().toLowerCase().contains(query)
                    && !user.getEmail().toLowerCase().contains(query)
                    && !(user.getRegistrationNumber() != null && user.getRegistrationNumber().toLowerCase().contains(query))
                    && !(user.getRollNumber() != null && user.getRollNumber().toLowerCase().contains(query))) {
                continue;
            }
            result.add(user);
        }
        UserSortHelper.sort(result, studentSortOption);
        return result;
    }

    public List<User> getVisibleStudents() {
        List<User> filtered = getFilteredStudents();
        return filtered.subList(0, Math.min(visibleStudentCount, filtered.size()));
    }

    public boolean isHasMoreStudents() {
        return getFilteredStudents().size() > visibleStudentCount;
    }
}
