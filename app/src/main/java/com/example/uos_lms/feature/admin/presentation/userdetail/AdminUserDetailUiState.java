package com.example.uos_lms.feature.admin.presentation.userdetail;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminUserDetailUiState {
    private final User user;
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    @Builder.Default
    private final List<Session> sessions = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;
    private final String actionMessage;
    private final HodConflict hodConflict;

    @Builder.Default
    private final boolean loadingCourses = false;
    @Builder.Default
    private final List<Subject> assignedCourses = Collections.emptyList();

    public static AdminUserDetailUiState initial() {
        return AdminUserDetailUiState.builder().build();
    }
}
