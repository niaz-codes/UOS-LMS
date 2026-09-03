package com.example.uos_lms.feature.admin.university.presentation.semester;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Leaf of the Admin Department Management hierarchy: Semester -> (Students + Subjects),
 * scoped to the (departmentId, sessionId, semesterId) selected via DepartmentDetail ->
 * SessionSemesterList -> here. Subjects stay Department+Semester scoped as before (no
 * session field) - sessionId is only used to scope the Students section.
 */
@HiltViewModel
public class SemesterSubjectsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthDataSource authDataSource;
    private final String departmentId;
    private final String semesterId;
    private final String sessionId;

    private final MutableLiveData<SemesterSubjectsUiState> uiState =
            new MutableLiveData<>(SemesterSubjectsUiState.initial());

    @Inject
    public SemesterSubjectsViewModel(
            SavedStateHandle savedStateHandle,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthDataSource authDataSource) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authDataSource = authDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        this.semesterId = savedStateHandle.get("semesterId");
        this.sessionId = savedStateHandle.get("sessionId");
        load();
    }

    public LiveData<SemesterSubjectsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches subjects, teachers, and students from the backend without blanking what's
     * currently shown, preserving the student search/filter - no-ops while a refresh is
     * already in flight. */
    public void refresh() {
        SemesterSubjectsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
            for (Semester candidate : semesters) {
                if (candidate.getId().equals(semesterId)) {
                    uiState.setValue(uiState.getValue().toBuilder().semester(candidate).build());
                    break;
                }
            }
        });
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (department.getId().equals(departmentId)) {
                    uiState.setValue(uiState.getValue().toBuilder().departmentName(department.getName()).build());
                    break;
                }
            }
        });
        universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
            for (Session session : sessions) {
                if (session.getId().equals(sessionId)) {
                    uiState.setValue(uiState.getValue().toBuilder().sessionLabel(session.getLabel()).build());
                    break;
                }
            }
        });
        userDataSource.listApprovedTeachersInDepartment(departmentId)
                .addOnSuccessListener(teachers -> uiState.setValue(uiState.getValue().toBuilder().teachers(teachers).build()));
        loadSubjects();
        loadStudents();
    }

    private void loadSubjects() {
        universityDataSource.listSubjectsForSemester(semesterId)
                .addOnSuccessListener(subjects -> {
                    List<Subject> sorted = new ArrayList<>(subjects);
                    sorted.sort((a, b) -> a.getCode().compareTo(b.getCode()));
                    uiState.setValue(uiState.getValue().toBuilder().subjects(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void loadStudents() {
        userDataSource.listStudentsInSession(departmentId, sessionId, semesterId)
                .addOnSuccessListener(students -> uiState.setValue(uiState.getValue().toBuilder().students(students).loadingStudents(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loadingStudents(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public Task<Subject> createSubject(String code, String title, int creditHours) {
        return universityDataSource.createSubject(departmentId, semesterId, code, title, creditHours)
                .addOnSuccessListener(v -> loadSubjects());
    }

    public Task<Subject> updateSubject(String id, String code, String title, int creditHours) {
        return universityDataSource.updateSubject(id, code, title, creditHours).addOnSuccessListener(v -> loadSubjects());
    }

    public void deleteSubject(String id) {
        universityDataSource.deleteSubject(id)
                .addOnSuccessListener(v -> loadSubjects())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void assignTeacher(String subjectId, User teacher) {
        universityDataSource.assignTeacher(subjectId, teacher.getUid())
                .addOnSuccessListener(v -> loadSubjects())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void unassignTeacher(String subjectId) {
        universityDataSource.unassignTeacher(subjectId)
                .addOnSuccessListener(v -> loadSubjects())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void onStudentSearchChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().studentSearchQuery(query).visibleStudentCount(SemesterSubjectsUiState.PAGE_SIZE).build());
    }

    public void onStudentFilterChange(UserFilter filter) {
        uiState.setValue(uiState.getValue().toBuilder().studentFilter(filter).visibleStudentCount(SemesterSubjectsUiState.PAGE_SIZE).build());
    }

    public void onStudentSortChange(UserSortOption sort) {
        uiState.setValue(uiState.getValue().toBuilder().studentSortOption(sort).build());
    }

    public void onStudentLoadMore() {
        uiState.setValue(uiState.getValue().toBuilder()
                .visibleStudentCount(uiState.getValue().getVisibleStudentCount() + SemesterSubjectsUiState.PAGE_SIZE)
                .build());
    }

    public void approveStudent(String uid) {
        updateStudentStatus(uid, UserStatus.APPROVED, null, "Student approved.");
    }

    public void rejectStudent(String uid) {
        updateStudentStatus(uid, UserStatus.REJECTED, "Rejected by Admin.", "Student rejected.");
    }

    public void suspendStudent(String uid) {
        updateStudentStatus(uid, UserStatus.SUSPENDED, "Suspended by Admin.", "Student suspended.");
    }

    public void activateStudent(String uid) {
        updateStudentStatus(uid, UserStatus.APPROVED, null, "Student activated.");
    }

    private void updateStudentStatus(String uid, UserStatus status, String reason, String successMessage) {
        userDataSource.updateStatus(uid, status, reason)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build());
                    loadStudents();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void deleteStudent(String uid) {
        userDataSource.deleteUser(uid)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Student deleted.").build());
                    loadStudents();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void resetStudentPassword(String email) {
        authDataSource.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage("A password reset code has been emailed to the student.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }

    public void consumeActionMessage() {
        uiState.setValue(uiState.getValue().toBuilder().actionMessage(null).build());
    }
}
