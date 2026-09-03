package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.UserRole;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Top level of the Student Management hierarchy: Department -> Session. Clicking a
 * Session navigates away to AdminStudentSemesterListFragment (Session -> Semester ->
 * Students) rather than expanding further inline, so a Session node here is a plain
 * leaf with no expand/student state of its own.
 */
@HiltViewModel
public class AdminStudentTreeViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;

    private final MutableLiveData<AdminStudentTreeUiState> uiState =
            new MutableLiveData<>(AdminStudentTreeUiState.initial());

    @Inject
    public AdminStudentTreeViewModel(ApiUniversityDataSource universityDataSource, ApiUserDataSource userDataSource) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        load();
    }

    public LiveData<AdminStudentTreeUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the department list, header stats, and every currently-expanded department's
     * sessions, without collapsing anything or losing a node's search - no-ops while a refresh
     * is already in flight. */
    public void refresh() {
        AdminStudentTreeUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.listDepartments().addOnSuccessListener(this::onDepartmentsLoaded);
        loadHeaderStats();
    }

    private void loadHeaderStats() {
        userDataSource.countByRole(UserRole.STUDENT).addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().totalStudents(count).build()));
        universityDataSource.countAllSemesters().addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().totalSemesters(count).build()));
        universityDataSource.countAllSessions().addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().totalSessions(count).build()));
    }

    private void onDepartmentsLoaded(List<Department> departments) {
        List<Department> sorted = new ArrayList<>(departments);
        sorted.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<String, DepartmentStudentNode> previousById = new HashMap<>();
        for (DepartmentStudentNode node : uiState.getValue().getNodes()) {
            previousById.put(node.getDepartment().getId(), node);
        }

        List<DepartmentStudentNode> nodes = new ArrayList<>();
        for (Department department : sorted) {
            DepartmentStudentNode previous = previousById.get(department.getId());
            nodes.add(previous != null ? previous.toBuilder().department(department).build() : DepartmentStudentNode.of(department));
            loadDepartmentCount(department.getId());
        }
        uiState.setValue(uiState.getValue().toBuilder().nodes(nodes).loadingDepartments(false).refreshing(false).build());

        for (DepartmentStudentNode node : nodes) {
            if (node.isExpanded()) {
                universityDataSource.listSessions(node.getDepartment().getId())
                        .addOnSuccessListener(sessions -> onSessionsLoaded(node.getDepartment().getId(), sessions));
            }
        }
    }

    private void loadDepartmentCount(String departmentId) {
        userDataSource.countInDepartmentByRole(departmentId, UserRole.STUDENT).addOnSuccessListener(count ->
                updateDept(departmentId, node -> node.toBuilder().totalCount(count).build()));
    }

    public void onDepartmentSearchChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().departmentSearchQuery(query).build());
    }

    public void toggleDepartment(String departmentId) {
        DepartmentStudentNode node = findDept(departmentId);
        if (node == null) return;
        if (node.isExpanded()) {
            updateDept(departmentId, n -> n.toBuilder().expanded(false).build());
            return;
        }
        updateDept(departmentId, n -> n.toBuilder().expanded(true).loadingSessions(true).build());
        universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> onSessionsLoaded(departmentId, sessions));
    }

    private void onSessionsLoaded(String departmentId, List<Session> sessions) {
        List<Session> sorted = new ArrayList<>(sessions);
        sorted.sort((a, b) -> b.getLabel().compareTo(a.getLabel()));

        List<SessionNode> nodes = new ArrayList<>();
        for (Session session : sorted) {
            nodes.add(SessionNode.builder().session(session).build());
        }
        updateDept(departmentId, n -> n.toBuilder().sessions(nodes).loadingSessions(false).build());
        for (Session session : sorted) {
            loadSessionCount(departmentId, session.getId());
        }
    }

    private void loadSessionCount(String departmentId, String sessionId) {
        userDataSource.countStudentsInSession(departmentId, sessionId, null).addOnSuccessListener(count ->
                updateSession(departmentId, sessionId, node -> node.toBuilder().totalCount(count).build()));
    }

    public void onSessionSearchChange(String departmentId, String query) {
        updateDept(departmentId, n -> n.toBuilder().sessionSearchQuery(query).build());
    }

    public Task<Session> createSession(String departmentId, String label) {
        return universityDataSource.createSession(departmentId, label).addOnSuccessListener(v -> loadHeaderStats());
    }

    private DepartmentStudentNode findDept(String departmentId) {
        for (DepartmentStudentNode node : uiState.getValue().getNodes()) {
            if (node.getDepartment().getId().equals(departmentId)) return node;
        }
        return null;
    }

    private void updateDept(String departmentId, Function<DepartmentStudentNode, DepartmentStudentNode> transform) {
        List<DepartmentStudentNode> nodes = new ArrayList<>();
        for (DepartmentStudentNode node : uiState.getValue().getNodes()) {
            nodes.add(node.getDepartment().getId().equals(departmentId) ? transform.apply(node) : node);
        }
        uiState.setValue(uiState.getValue().toBuilder().nodes(nodes).build());
    }

    private void updateSession(String departmentId, String sessionId, Function<SessionNode, SessionNode> transform) {
        updateDept(departmentId, dept -> {
            List<SessionNode> sessions = new ArrayList<>();
            for (SessionNode node : dept.getSessions()) {
                sessions.add(node.getSession().getId().equals(sessionId) ? transform.apply(node) : node);
            }
            return dept.toBuilder().sessions(sessions).build();
        });
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
