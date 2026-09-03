package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.example.uos_lms.feature.auth.data.AuthDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Bottom of the Student Management hierarchy: Session -> Semester -> Students, scoped
 * to the (departmentId, sessionId) the admin drilled into from AdminStudentTreeFragment.
 */
@HiltViewModel
public class AdminStudentSemesterListViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthDataSource authDataSource;
    private final String departmentId;
    private final String sessionId;

    private final MutableLiveData<AdminStudentSemesterListUiState> uiState =
            new MutableLiveData<>(AdminStudentSemesterListUiState.initial());

    @Inject
    public AdminStudentSemesterListViewModel(
            SavedStateHandle savedStateHandle,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthDataSource authDataSource) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authDataSource = authDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        this.sessionId = savedStateHandle.get("sessionId");

        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            String name = "";
            for (Department department : departments) {
                if (department.getId().equals(departmentId)) {
                    name = department.getName();
                    break;
                }
            }
            uiState.setValue(uiState.getValue().toBuilder().departmentName(name).build());
        });

        universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
            String label = "";
            for (Session session : sessions) {
                if (session.getId().equals(sessionId)) {
                    label = session.getLabel();
                    break;
                }
            }
            uiState.setValue(uiState.getValue().toBuilder().sessionLabel(label).build());
        });

        universityDataSource.listSemesters(departmentId).addOnSuccessListener(this::onSemestersLoaded);
    }

    public LiveData<AdminStudentSemesterListUiState> getUiState() {
        return uiState;
    }

    private void onSemestersLoaded(List<Semester> semesters) {
        List<Semester> sorted = new ArrayList<>(semesters);
        sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));

        List<SemesterStudentsNode> nodes = new ArrayList<>();
        for (Semester semester : sorted) {
            nodes.add(SemesterStudentsNode.of(semester));
            loadSemesterCount(semester.getId());
        }
        uiState.setValue(uiState.getValue().toBuilder().semesters(nodes).loading(false).build());
    }

    private void loadSemesterCount(String semesterId) {
        userDataSource.countStudentsInSession(departmentId, sessionId, semesterId).addOnSuccessListener(count ->
                updateSemester(semesterId, node -> node.toBuilder().totalCount(count).build()));
    }

    public void onSemesterSearchChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().semesterSearchQuery(query).build());
    }

    public void toggleSemester(String semesterId) {
        SemesterStudentsNode node = findSemester(semesterId);
        if (node == null) return;
        if (node.isExpanded()) {
            updateSemester(semesterId, n -> n.toBuilder().expanded(false).build());
            return;
        }
        updateSemester(semesterId, n -> n.toBuilder().expanded(true).loadingStudents(true).build());
        userDataSource.listStudentsInSession(departmentId, sessionId, semesterId)
                .addOnSuccessListener(students -> updateSemester(semesterId, n -> n.toBuilder().students(students).loadingStudents(false).build()))
                .addOnFailureListener(e -> updateSemester(semesterId, n -> n.toBuilder().loadingStudents(false).build()));
    }

    public void onStudentSearchChange(String semesterId, String query) {
        updateSemester(semesterId, n -> n.toBuilder().searchQuery(query).visibleCount(SemesterStudentsNode.PAGE_SIZE).build());
    }

    public void onStudentFilterChange(String semesterId, UserFilter filter) {
        updateSemester(semesterId, n -> n.toBuilder().filter(filter).visibleCount(SemesterStudentsNode.PAGE_SIZE).build());
    }

    public void onStudentSortChange(String semesterId, UserSortOption sort) {
        updateSemester(semesterId, n -> n.toBuilder().sortOption(sort).build());
    }

    public void onStudentLoadMore(String semesterId) {
        updateSemester(semesterId, n -> n.toBuilder().visibleCount(n.getVisibleCount() + SemesterStudentsNode.PAGE_SIZE).build());
    }

    private SemesterStudentsNode findSemester(String semesterId) {
        for (SemesterStudentsNode node : uiState.getValue().getSemesters()) {
            if (node.getSemester().getId().equals(semesterId)) return node;
        }
        return null;
    }

    private void updateSemester(String semesterId, Function<SemesterStudentsNode, SemesterStudentsNode> transform) {
        List<SemesterStudentsNode> nodes = new ArrayList<>();
        for (SemesterStudentsNode node : uiState.getValue().getSemesters()) {
            nodes.add(node.getSemester().getId().equals(semesterId) ? transform.apply(node) : node);
        }
        uiState.setValue(uiState.getValue().toBuilder().semesters(nodes).build());
    }

    public void approve(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, "Student approved.");
    }

    public void reject(String uid) {
        updateStatus(uid, UserStatus.REJECTED, "Rejected by Admin.", "Student rejected.");
    }

    public void suspend(String uid) {
        updateStatus(uid, UserStatus.SUSPENDED, "Suspended by Admin.", "Student suspended.");
    }

    public void activate(String uid) {
        updateStatus(uid, UserStatus.APPROVED, null, "Student activated.");
    }

    private void updateStatus(String uid, UserStatus status, String reason, String successMessage) {
        userDataSource.updateStatus(uid, status, reason)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete(String uid, String semesterId) {
        userDataSource.deleteUser(uid)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Student deleted.").build());
                    loadSemesterCount(semesterId);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void resetPassword(String email) {
        authDataSource.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage("A password reset code has been emailed to the student.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
