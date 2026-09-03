package com.example.uos_lms.feature.admin.university.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.core.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Drives the Admin Dashboard: one allUsers() call derives Total Students/Teachers/HODs (plus a
 * genuine "created this calendar month" delta per role) and the Pending Approvals list, and one
 * listDepartments() call derives Total Departments (+ its own delta) and the id->name lookup the
 * pending rows use for their department line. No dedicated backend aggregate exists for any of
 * this, so it's all computed client-side from the real records - never fabricated. */
@HiltViewModel
public class AdminHomeViewModel extends ViewModel {

    private static final int MAX_PENDING_ROWS = 5;

    private final ApiUserDataSource userDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final SessionManager sessionManager;

    private final MutableLiveData<AdminHomeUiState> uiState = new MutableLiveData<>(AdminHomeUiState.initial());

    private List<User> allUsers = Collections.emptyList();
    private Map<String, String> departmentNamesById = Collections.emptyMap();
    private boolean usersLoaded;
    private boolean departmentsLoaded;

    @Inject
    public AdminHomeViewModel(ApiUserDataSource userDataSource, ApiUniversityDataSource universityDataSource,
                               SessionManager sessionManager) {
        this.userDataSource = userDataSource;
        this.universityDataSource = universityDataSource;
        this.sessionManager = sessionManager;
        load();
    }

    public LiveData<AdminHomeUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches both the users and departments feeds this dashboard is built from, without
     * blanking anything already on screen - no-ops while a refresh is already in flight. */
    public void refresh() {
        AdminHomeUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        userDataSource.allUsers()
                .addOnSuccessListener(users -> {
                    allUsers = users;
                    usersLoaded = true;
                    recompute();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loading(false).refreshing(false).errorMessage(e.getMessage()).build()));

        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> {
                    Map<String, String> names = new HashMap<>();
                    for (Department department : departments) names.put(department.getId(), department.getName());
                    departmentNamesById = names;
                    departmentsLoaded = true;

                    int total = departments.size();
                    int delta = 0;
                    for (Department department : departments) {
                        if (isThisMonth(department.getCreatedAt())) delta++;
                    }
                    uiState.setValue(uiState.getValue().toBuilder()
                            .totalDepartments(total).departmentsDelta(delta)
                            .allDepartments(departments).build());
                    recompute();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().refreshing(false).errorMessage(e.getMessage()).build()));
    }

    /** Re-derives every users-driven field from the current allUsers cache - called after the
     * initial load and again after an optimistic local approve/reject patch. Department-name
     * resolution degrades gracefully if departments haven't loaded yet (shows role only); the
     * departments callback re-runs this once they arrive so names backfill without a refetch. */
    private void recompute() {
        if (!usersLoaded) return;

        int totalStudents = 0, studentsDelta = 0;
        int totalTeachers = 0, teachersDelta = 0;
        int totalHods = 0, hodsDelta = 0;
        List<User> pending = new ArrayList<>();

        for (User user : allUsers) {
            boolean thisMonth = isThisMonth(user.getCreatedAt());
            if (user.getRole() == UserRole.STUDENT) {
                totalStudents++;
                if (thisMonth) studentsDelta++;
            } else if (user.getRole() == UserRole.TEACHER) {
                totalTeachers++;
                if (thisMonth) teachersDelta++;
            } else if (user.getRole() == UserRole.HOD) {
                totalHods++;
                if (thisMonth) hodsDelta++;
            }
            if (user.getStatus() == UserStatus.PENDING) pending.add(user);
        }

        pending.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        List<PendingApprovalRow> rows = new ArrayList<>();
        for (User user : pending.subList(0, Math.min(MAX_PENDING_ROWS, pending.size()))) {
            rows.add(PendingApprovalRow.builder()
                    .uid(user.getUid())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .departmentName(departmentsLoaded ? departmentNamesById.get(user.getDepartment()) : null)
                    .createdAt(user.getCreatedAt())
                    .build());
        }

        AdminHomeUiState current = uiState.getValue();
        uiState.setValue(current.toBuilder()
                .loading(false)
                .refreshing(false)
                .totalStudents(totalStudents).studentsDelta(studentsDelta)
                .totalTeachers(totalTeachers).teachersDelta(teachersDelta)
                .totalHods(totalHods).hodsDelta(hodsDelta)
                .pendingApprovalsCount(pending.size())
                .pendingApprovals(rows)
                .teachersRoster(filterUsersByRole(UserRole.TEACHER, current.getTeachersDepartmentFilterId()))
                .hodsRoster(filterUsersByRole(UserRole.HOD, current.getHodsDepartmentFilterId()))
                .build());
    }

    private static boolean isThisMonth(long createdAtMillis) {
        if (createdAtMillis <= 0) return false;
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(createdAtMillis);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == then.get(Calendar.MONTH);
    }

    public void approve(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, "User approved.");
    }

    public void reject(String uid) {
        updateStatus(uid, UserStatus.REJECTED, "Rejected by Admin.", "User rejected.");
    }

    private void updateStatus(String uid, UserStatus status, String reason, String successMessage) {
        userDataSource.updateStatus(uid, status, reason)
                .addOnSuccessListener(updated -> {
                    List<User> next = new ArrayList<>();
                    for (User user : allUsers) {
                        next.add(user.getUid().equals(updated.getUid()) ? updated : user);
                    }
                    allUsers = next;
                    recompute();
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }

    public void logout() {
        sessionManager.clear();
    }

    // ---- Students drill-down: Department -> Session -> Semester -> roster ----

    public void toggleStudentsSection() {
        AdminHomeUiState s = uiState.getValue();
        uiState.setValue(s.toBuilder().studentsSectionExpanded(!s.isStudentsSectionExpanded()).build());
    }

    public void selectStudentsDepartment(String departmentId) {
        AdminHomeUiState s = uiState.getValue();
        if (departmentId == null || departmentId.equals(s.getStudentsDepartmentId())) return;
        uiState.setValue(s.toBuilder()
                .studentsDepartmentId(departmentId)
                .studentsSessionId(null)
                .studentsSemesterId(null)
                .studentsSessions(Collections.<Session>emptyList())
                .studentsSemesters(Collections.<Semester>emptyList())
                .studentsRoster(Collections.<User>emptyList())
                .studentsSessionsLoading(true)
                .studentsSemestersLoading(true)
                .build());

        universityDataSource.listSessions(departmentId).addOnCompleteListener(task -> {
            AdminHomeUiState current = uiState.getValue();
            if (!departmentId.equals(current.getStudentsDepartmentId())) return; // stale response
            uiState.setValue(current.toBuilder()
                    .studentsSessionsLoading(false)
                    .studentsSessions(task.isSuccessful() && task.getResult() != null ? task.getResult() : Collections.<Session>emptyList())
                    .build());
        });
        universityDataSource.listSemesters(departmentId).addOnCompleteListener(task -> {
            AdminHomeUiState current = uiState.getValue();
            if (!departmentId.equals(current.getStudentsDepartmentId())) return; // stale response
            uiState.setValue(current.toBuilder()
                    .studentsSemestersLoading(false)
                    .studentsSemesters(task.isSuccessful() && task.getResult() != null ? task.getResult() : Collections.<Semester>emptyList())
                    .build());
        });
    }

    public void selectStudentsSession(String sessionId) {
        AdminHomeUiState s = uiState.getValue();
        if (sessionId == null || sessionId.equals(s.getStudentsSessionId())) return;
        uiState.setValue(s.toBuilder()
                .studentsSessionId(sessionId)
                .studentsSemesterId(null)
                .studentsRoster(Collections.<User>emptyList())
                .build());
    }

    public void selectStudentsSemester(String semesterId) {
        AdminHomeUiState s = uiState.getValue();
        if (semesterId == null || semesterId.equals(s.getStudentsSemesterId())) return;
        String departmentId = s.getStudentsDepartmentId();
        String sessionId = s.getStudentsSessionId();
        uiState.setValue(s.toBuilder()
                .studentsSemesterId(semesterId)
                .studentsRoster(Collections.<User>emptyList())
                .studentsRosterLoading(true)
                .build());

        userDataSource.listStudentsInSession(departmentId, sessionId, semesterId).addOnCompleteListener(task -> {
            AdminHomeUiState current = uiState.getValue();
            if (!semesterId.equals(current.getStudentsSemesterId())) return; // stale response
            uiState.setValue(current.toBuilder()
                    .studentsRosterLoading(false)
                    .studentsRoster(task.isSuccessful() && task.getResult() != null ? task.getResult() : Collections.<User>emptyList())
                    .build());
        });
    }

    // ---- Teachers: shown in full immediately (filtered from the already-cached allUsers list -
    // no network round trip), with an optional Department filter that re-filters instantly. A
    // teacher with several departments legitimately reappears under more than one filter. ----

    public void toggleTeachersSection() {
        AdminHomeUiState s = uiState.getValue();
        uiState.setValue(s.toBuilder().teachersSectionExpanded(!s.isTeachersSectionExpanded()).build());
    }

    public void selectTeachersDepartmentFilter(String departmentId) {
        AdminHomeUiState s = uiState.getValue();
        if (isSame(departmentId, s.getTeachersDepartmentFilterId())) return;
        uiState.setValue(s.toBuilder()
                .teachersDepartmentFilterId(departmentId)
                .teachersRoster(filterUsersByRole(UserRole.TEACHER, departmentId))
                .build());
    }

    // ---- HODs: shown in full immediately, same optional Department filter ----

    public void toggleHodsSection() {
        AdminHomeUiState s = uiState.getValue();
        uiState.setValue(s.toBuilder().hodsSectionExpanded(!s.isHodsSectionExpanded()).build());
    }

    public void selectHodsDepartmentFilter(String departmentId) {
        AdminHomeUiState s = uiState.getValue();
        if (isSame(departmentId, s.getHodsDepartmentFilterId())) return;
        uiState.setValue(s.toBuilder()
                .hodsDepartmentFilterId(departmentId)
                .hodsRoster(filterUsersByRole(UserRole.HOD, departmentId))
                .build());
    }

    /** null departmentId means "All". TEACHER matches via the many-to-many departmentIds list;
     * HOD (and everything else) via the single department field. */
    private List<User> filterUsersByRole(UserRole role, String departmentId) {
        List<User> result = new ArrayList<>();
        for (User user : allUsers) {
            if (user.getRole() != role) continue;
            if (departmentId == null) {
                result.add(user);
            } else if (role == UserRole.TEACHER ? user.getDepartmentIds().contains(departmentId)
                    : departmentId.equals(user.getDepartment())) {
                result.add(user);
            }
        }
        return result;
    }

    private static boolean isSame(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
