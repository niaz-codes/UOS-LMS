package com.example.uos_lms.feature.admin.presentation.usermanagement.common;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.example.uos_lms.feature.auth.data.AuthDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared logic behind the Admin HOD/Teacher user-management tabs - an expandable-by-the
 * department tree, each department lazily loading its role-scoped member list with its own
 * search/filter/sort/pagination. Not a Hilt entry point itself; concrete per-role subclasses
 * (AdminHodTreeViewModel/AdminTeacherTreeViewModel) supply the Hilt @Inject constructor and
 * fix which [UserRole] this instance is scoped to. */
public abstract class AdminRoleTreeViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthDataSource authDataSource;
    private final UserRole role;

    protected final MutableLiveData<AdminRoleTreeUiState> uiState =
            new MutableLiveData<>(AdminRoleTreeUiState.initial());

    protected AdminRoleTreeViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthDataSource authDataSource,
            UserRole role) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authDataSource = authDataSource;
        this.role = role;
        load();
    }

    public LiveData<AdminRoleTreeUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the department list, per-department counts, and every currently-expanded
     * department's member list, without collapsing anything or losing a node's search/filter -
     * no-ops while a refresh is already in flight. */
    public void refresh() {
        AdminRoleTreeUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.listDepartments().addOnSuccessListener(this::onDepartmentsLoaded);

        userDataSource.countByRole(role).addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().totalCount(count).build()));
    }

    private void onDepartmentsLoaded(List<Department> departments) {
        List<Department> sorted = new ArrayList<>(departments);
        sorted.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<String, DepartmentUserNode> previousById = new HashMap<>();
        for (DepartmentUserNode node : uiState.getValue().getNodes()) {
            previousById.put(node.getDepartment().getId(), node);
        }

        List<DepartmentUserNode> nodes = new ArrayList<>();
        for (Department department : sorted) {
            DepartmentUserNode previous = previousById.get(department.getId());
            nodes.add(previous != null ? previous.toBuilder().department(department).build() : DepartmentUserNode.of(department));
            loadCount(department.getId());
        }
        uiState.setValue(uiState.getValue().toBuilder().nodes(nodes).loadingDepartments(false).refreshing(false).build());

        for (DepartmentUserNode node : nodes) {
            if (node.isExpanded()) loadMembers(node.getDepartment().getId());
        }
    }

    private void loadCount(String departmentId) {
        userDataSource.countInDepartmentByRole(departmentId, role).addOnSuccessListener(count ->
                updateNode(departmentId, node -> node.toBuilder().totalCount(count).build()));
    }

    public void onDepartmentSearchChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().departmentSearchQuery(query).build());
    }

    public void toggleDepartment(String departmentId) {
        DepartmentUserNode node = findNode(departmentId);
        if (node == null) return;
        if (node.isExpanded()) {
            updateNode(departmentId, n -> n.toBuilder().expanded(false).build());
            return;
        }
        updateNode(departmentId, n -> n.toBuilder().expanded(true).build());
        loadMembers(departmentId);
    }

    private void loadMembers(String departmentId) {
        updateNode(departmentId, n -> n.toBuilder().loadingUsers(true).build());
        userDataSource.listUsersInDepartmentByRole(departmentId, role).addOnSuccessListener(users ->
                updateNode(departmentId, n -> n.toBuilder().users(users).loadingUsers(false).build()));
    }

    public void onNodeSearchChange(String departmentId, String query) {
        updateNode(departmentId, n -> n.toBuilder().searchQuery(query).visibleCount(DepartmentUserNode.PAGE_SIZE).build());
    }

    public void onNodeFilterChange(String departmentId, UserFilter filter) {
        updateNode(departmentId, n -> n.toBuilder().filter(filter).visibleCount(DepartmentUserNode.PAGE_SIZE).build());
    }

    public void onNodeSortChange(String departmentId, UserSortOption sort) {
        updateNode(departmentId, n -> n.toBuilder().sortOption(sort).build());
    }

    public void onNodeLoadMore(String departmentId) {
        updateNode(departmentId, n -> n.toBuilder().visibleCount(n.getVisibleCount() + DepartmentUserNode.PAGE_SIZE).build());
    }

    private DepartmentUserNode findNode(String departmentId) {
        for (DepartmentUserNode node : uiState.getValue().getNodes()) {
            if (node.getDepartment().getId().equals(departmentId)) return node;
        }
        return null;
    }

    private void updateNode(String departmentId, java.util.function.Function<DepartmentUserNode, DepartmentUserNode> transform) {
        List<DepartmentUserNode> nodes = new ArrayList<>();
        for (DepartmentUserNode node : uiState.getValue().getNodes()) {
            nodes.add(node.getDepartment().getId().equals(departmentId) ? transform.apply(node) : node);
        }
        uiState.setValue(uiState.getValue().toBuilder().nodes(nodes).build());
    }

    public void approve(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, roleLabel() + " approved.");
    }

    public void reject(String uid) {
        updateStatus(uid, UserStatus.REJECTED, "Rejected by Admin.", roleLabel() + " rejected.");
    }

    public void suspend(String uid) {
        updateStatus(uid, UserStatus.SUSPENDED, "Suspended by Admin.", roleLabel() + " suspended.");
    }

    public void activate(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, roleLabel() + " activated.");
    }

    private void updateStatus(String uid, UserStatus status, String reason, String successMessage) {
        userDataSource.updateStatus(uid, status, reason)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete(String uid, String departmentId) {
        userDataSource.deleteUser(uid)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage(roleLabel() + " deleted.").build());
                    loadCount(departmentId);
                    loadMembers(departmentId);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void resetPassword(String email) {
        authDataSource.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage("A password reset code has been emailed to the user.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }

    protected abstract String roleLabel();
}
