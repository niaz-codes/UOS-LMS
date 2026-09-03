package com.example.uos_lms.feature.announcement.presentation;

import com.example.uos_lms.core.domain.model.Announcement;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.UserRole;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AnnouncementsUiState {
    @Builder.Default
    private final List<Announcement> announcements = Collections.emptyList();
    private final UserRole role;
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final List<Subject> mySubjects = Collections.emptyList();
    private final String myDepartmentId;
    private final String myDepartmentName;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean posting = false;
    private final String errorMessage;

    public static AnnouncementsUiState initial() {
        return AnnouncementsUiState.builder().build();
    }
}
