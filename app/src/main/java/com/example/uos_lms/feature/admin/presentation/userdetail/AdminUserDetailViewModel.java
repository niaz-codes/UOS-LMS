package com.example.uos_lms.feature.admin.presentation.userdetail;

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
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.feature.auth.data.AuthDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminUserDetailViewModel extends ViewModel {

    private final ApiUserDataSource userDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final AuthDataSource authDataSource;
    private final String uid;

    private final MutableLiveData<AdminUserDetailUiState> uiState =
            new MutableLiveData<>(AdminUserDetailUiState.initial());

    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    @Inject
    public AdminUserDetailViewModel(
            SavedStateHandle savedStateHandle,
            ApiUserDataSource userDataSource,
            ApiUniversityDataSource universityDataSource,
            AuthDataSource authDataSource) {
        this.userDataSource = userDataSource;
        this.universityDataSource = universityDataSource;
        this.authDataSource = authDataSource;
        this.uid = savedStateHandle.get("uid");

        universityDataSource.listDepartments().addOnSuccessListener(departments ->
                uiState.setValue(uiState.getValue().toBuilder().departments(departments).build()));

        loadUser();
    }

    public LiveData<AdminUserDetailUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    private void loadUser() {
        userDataSource.getUser(uid)
                .addOnSuccessListener(user -> {
                    uiState.setValue(uiState.getValue().toBuilder().user(user).loading(false).build());
                    if (user != null && user.getDepartment() != null) {
                        loadSemestersForDepartment(user.getDepartment());
                        loadSessionsForDepartment(user.getDepartment());
                    } else {
                        uiState.setValue(uiState.getValue().toBuilder()
                                .semesters(Collections.emptyList()).sessions(Collections.emptyList()).build());
                    }
                    if (user != null && user.getRole() == UserRole.TEACHER) {
                        loadAssignedCourses();
                    }
                })
                .addOnFailureListener(e ->
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    // ---- Overview tab ----

    // ---- Assign Course (Teacher) ----

    private void loadAssignedCourses() {
        uiState.setValue(uiState.getValue().toBuilder().loadingCourses(true).build());
        universityDataSource.listSubjectsForTeacher(uid)
                .addOnSuccessListener(subjects -> uiState.setValue(uiState.getValue().toBuilder()
                        .loadingCourses(false).assignedCourses(subjects).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loadingCourses(false).errorMessage(e.getMessage()).build()));
    }

    public Task<List<Department>> listDepartmentsForCourseAssign() {
        return universityDataSource.listDepartments();
    }

    public Task<List<Semester>> listSemestersForCourseAssign(String departmentId) {
        return universityDataSource.listSemesters(departmentId);
    }

    public Task<List<Subject>> listSubjectsForCourseAssign(String semesterId) {
        return universityDataSource.listSubjectsForSemester(semesterId);
    }

    public void assignCourse(Subject subject) {
        universityDataSource.assignTeacher(subject.getId(), uid)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Course assigned.").build());
                    loadAssignedCourses();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void removeCourse(Subject subject) {
        universityDataSource.unassignTeacher(subject.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Course removed.").build());
                    loadAssignedCourses();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    private void loadSemestersForDepartment(String departmentId) {
        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters ->
                uiState.setValue(uiState.getValue().toBuilder().semesters(semesters).build()));
    }

    private void loadSessionsForDepartment(String departmentId) {
        universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions ->
                uiState.setValue(uiState.getValue().toBuilder().sessions(sessions).build()));
    }

    private void updateStatus(UserStatus newStatus, String reason, String successMessage) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateStatus(user.getUid(), newStatus, reason)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage(successMessage).build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void approve() {
        updateStatus(UserStatus.APPROVED, null, "User approved.");
    }

    public void reject() {
        updateStatus(UserStatus.REJECTED, "Rejected by Admin.", "User rejected.");
    }

    public void suspendUser() {
        updateStatus(UserStatus.SUSPENDED, "Suspended by Admin.", "User suspended.");
    }

    public void activate() {
        updateStatus(UserStatus.APPROVED, null, "User activated.");
    }

    public void resetPassword() {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        authDataSource.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(v -> uiState.setValue(uiState.getValue().toBuilder().actionMessage("A password reset code has been emailed to the user.").build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void delete() {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.deleteUser(user.getUid())
                .addOnSuccessListener(v -> deleted.setValue(true))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void requestAssignDepartment(Department department) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        if (user.getRole() != UserRole.HOD) {
            applyDepartment(department.getId());
            return;
        }
        userDataSource.findHodForDepartment(department.getId())
                .addOnSuccessListener(existingHod -> {
                    if (existingHod != null && !existingHod.getUid().equals(user.getUid())) {
                        uiState.setValue(uiState.getValue().toBuilder()
                                .hodConflict(HodConflict.builder()
                                        .departmentId(department.getId())
                                        .departmentName(department.getName())
                                        .existingHodUid(existingHod.getUid())
                                        .existingHodName(existingHod.getFullName())
                                        .build())
                                .build());
                    } else {
                        applyDepartment(department.getId());
                    }
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void confirmHodReassignment() {
        HodConflict conflict = uiState.getValue().getHodConflict();
        User user = uiState.getValue().getUser();
        if (conflict == null || user == null) return;
        uiState.setValue(uiState.getValue().toBuilder().hodConflict(null).build());
        userDataSource.unassignUserDepartment(conflict.getExistingHodUid())
                .continueWithTask(task -> userDataSource.updateUserDepartment(user.getUid(), conflict.getDepartmentId()))
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Department updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void cancelHodConflict() {
        uiState.setValue(uiState.getValue().toBuilder().hodConflict(null).build());
    }

    public void unassignDepartment() {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.unassignUserDepartment(user.getUid())
                .continueWithTask(task -> maybeInvalidateSemester(user, null))
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Department updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    private void applyDepartment(String departmentId) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateUserDepartment(user.getUid(), departmentId)
                .continueWithTask(task -> maybeInvalidateSemester(user, departmentId))
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Department updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    /** A Student's semester belongs to their old department; a department change invalidates
     * it rather than leaving a stale cross-department reference. */
    private com.google.android.gms.tasks.Task<User> maybeInvalidateSemester(User user, String newDepartmentId) {
        if (user.getRole() == UserRole.STUDENT && user.getSemester() != null
                && !Objects.equals(newDepartmentId, user.getDepartment())) {
            return userDataSource.unassignSemester(user.getUid());
        }
        return Tasks.forResult(null);
    }

    public void assignDepartments(List<String> departmentIds) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateUserDepartments(user.getUid(), departmentIds)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Departments updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void unassignAllDepartments() {
        assignDepartments(Collections.emptyList());
    }

    public void assignSemester(Semester semester) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.assignSemester(user.getUid(), semester.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Semester updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void unassignSemester() {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.unassignSemester(user.getUid())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Semester unassigned.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void updateProfile(String fullName, String fatherName, String phone, String cnic) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateProfile(user.getUid(), fullName, fatherName, phone, cnic)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Profile updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void updateIdentifiers(String employeeId, String registrationNumber, String rollNumber, String designation) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateIdentifiers(user.getUid(), employeeId, designation, registrationNumber, rollNumber)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Profile updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void assignSession(Session session) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.assignSession(user.getUid(), session.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Session updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void unassignSession() {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.unassignSession(user.getUid())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Session unassigned.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void changeRole(UserRole role) {
        User user = uiState.getValue().getUser();
        if (user == null) return;
        userDataSource.updateRole(user.getUid(), role)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Role updated.").build());
                    loadUser();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
