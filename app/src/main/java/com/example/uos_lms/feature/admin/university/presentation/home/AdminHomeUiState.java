package com.example.uos_lms.feature.admin.university.presentation.home;

import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** UI state for the Admin Dashboard's premium Overview (4 role/department totals + this-month
 * deltas), the Students/Teachers/HODs inline drill-down browsers, and the Pending Approvals
 * (a short, most-recent-first list with inline approve/reject) section. Every number and row
 * here comes straight from real API responses - deltas are a genuine count of records created
 * in the current calendar month, drill-down results are the real roster for the exact
 * department/session/semester picked, never a fabricated figure. */
@Getter
@Builder(toBuilder = true)
public class AdminHomeUiState {
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean refreshing = false;
    private final String errorMessage;
    private final String actionMessage;

    private final int totalStudents;
    private final int studentsDelta;
    private final int totalTeachers;
    private final int teachersDelta;
    private final int totalHods;
    private final int hodsDelta;
    private final int totalDepartments;
    private final int departmentsDelta;

    /** Shared across all three drill-downs below - loaded once for the Overview department
     * count and reused so picking a department never needs a second network round trip. */
    @Builder.Default
    private final List<Department> allDepartments = Collections.emptyList();

    private final int pendingApprovalsCount;
    @Builder.Default
    private final List<PendingApprovalRow> pendingApprovals = Collections.emptyList();

    // ---- Students: Department -> Session -> Semester -> roster ----
    @Builder.Default
    private final boolean studentsSectionExpanded = false;
    private final String studentsDepartmentId;
    private final String studentsSessionId;
    private final String studentsSemesterId;
    @Builder.Default
    private final List<Session> studentsSessions = Collections.emptyList();
    @Builder.Default
    private final List<Semester> studentsSemesters = Collections.emptyList();
    @Builder.Default
    private final boolean studentsSessionsLoading = false;
    @Builder.Default
    private final boolean studentsSemestersLoading = false;
    @Builder.Default
    private final boolean studentsRosterLoading = false;
    @Builder.Default
    private final List<User> studentsRoster = Collections.emptyList();

    // ---- Teachers: shown in full immediately (already in the allUsers cache), with an
    // optional Department filter (null = "All") that re-filters instantly, client-side - a
    // teacher with several departments can legitimately show up under more than one filter. ----
    @Builder.Default
    private final boolean teachersSectionExpanded = false;
    private final String teachersDepartmentFilterId;
    @Builder.Default
    private final List<User> teachersRoster = Collections.emptyList();

    // ---- HODs: shown in full immediately, same optional Department filter (null = "All") ----
    @Builder.Default
    private final boolean hodsSectionExpanded = false;
    private final String hodsDepartmentFilterId;
    @Builder.Default
    private final List<User> hodsRoster = Collections.emptyList();

    public static AdminHomeUiState initial() {
        return AdminHomeUiState.builder().build();
    }
}
