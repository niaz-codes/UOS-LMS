package com.example.uos_lms.feature.student.presentation.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentDashboardViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiAssignmentDataSource assignmentDataSource;
    private final AuthApi authApi;
    private final SessionManager sessionManager;

    private final MutableLiveData<StudentDashboardUiState> uiState =
            new MutableLiveData<>(StudentDashboardUiState.initial());

    private List<Subject> latestSubjects;
    private final Map<String, List<AttendanceRecord>> attendanceBySubject = new HashMap<>();
    private final Map<String, List<Assignment>> assignmentsBySubject = new HashMap<>();
    private List<AssignmentSubmission> latestSubmissions = Collections.emptyList();

    @Inject
    public StudentDashboardViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiAttendanceDataSource attendanceDataSource,
            ApiAssignmentDataSource assignmentDataSource,
            AuthApi authApi,
            SessionManager sessionManager) {
        this.universityDataSource = universityDataSource;
        this.attendanceDataSource = attendanceDataSource;
        this.assignmentDataSource = assignmentDataSource;
        this.authApi = authApi;
        this.sessionManager = sessionManager;
        load();
    }

    public LiveData<StudentDashboardUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches everything on this dashboard from the backend without blanking the currently-
     * shown stats/subjects - no-ops while a refresh is already in flight. */
    public void refresh() {
        StudentDashboardUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage("Session expired. Please log in again.").build());
                        return;
                    }
                    uiState.setValue(uiState.getValue().toBuilder()
                            .fullName(user.getFullName())
                            .departmentId(user.getDepartment())
                            .semesterId(user.getSemester())
                            .build());
                    String departmentId = user.getDepartment();
                    String semesterId = user.getSemester();
                    if (departmentId == null || semesterId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    loadSubjects(departmentId, semesterId, user.getUid());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void loadSubjects(String departmentId, String semesterId, String uid) {
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (department.getId().equals(departmentId)) {
                    uiState.setValue(uiState.getValue().toBuilder().departmentName(department.getName()).build());
                    break;
                }
            }
        });

        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
            for (Semester semester : semesters) {
                if (semester.getId().equals(semesterId)) {
                    uiState.setValue(uiState.getValue().toBuilder().semesterLabel(semester.getDisplayName()).build());
                    break;
                }
            }
        });

        universityDataSource.mySubjects().addOnSuccessListener(subjects -> {
            List<Subject> sorted = new ArrayList<>(subjects);
            sorted.sort(Comparator.comparing(Subject::getCode));
            latestSubjects = sorted;
            uiState.setValue(uiState.getValue().toBuilder().subjects(sorted).loading(false).refreshing(false).build());

            List<Task<?>> tasks = new ArrayList<>();
            for (Subject subject : sorted) {
                tasks.add(attendanceDataSource.studentAttendanceForSubject(subject.getId(), uid)
                        .addOnSuccessListener(records -> attendanceBySubject.put(subject.getId(), records)));
                tasks.add(assignmentDataSource.assignmentsForSubject(subject.getId())
                        .addOnSuccessListener(assignments -> assignmentsBySubject.put(subject.getId(), assignments)));
            }
            Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recomputeOverview());
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));

        assignmentDataSource.submissionsForCurrentStudent().addOnSuccessListener(submissions -> {
            latestSubmissions = submissions;
            recomputeOverview();
        });
    }

    private void recomputeOverview() {
        if (latestSubjects == null) return;

        int totalRecords = 0;
        int presentRecords = 0;
        List<Assignment> allAssignments = new ArrayList<>();
        for (Subject subject : latestSubjects) {
            List<AttendanceRecord> records = attendanceBySubject.get(subject.getId());
            if (records != null) {
                for (AttendanceRecord record : records) {
                    totalRecords++;
                    if (record.getStatus() == AttendanceStatus.PRESENT) presentRecords++;
                }
            }
            List<Assignment> assignments = assignmentsBySubject.get(subject.getId());
            if (assignments != null) allAssignments.addAll(assignments);
        }
        int percentage = totalRecords == 0 ? 0 : (presentRecords * 100) / totalRecords;

        Set<String> submittedIds = new HashSet<>();
        for (AssignmentSubmission submission : latestSubmissions) submittedIds.add(submission.getAssignmentId());
        int pending = 0;
        for (Assignment assignment : allAssignments) {
            if (!submittedIds.contains(assignment.getId())) pending++;
        }

        uiState.setValue(uiState.getValue().toBuilder().attendancePercentage(percentage).pendingAssignments(pending).build());
    }

    public void logout() {
        sessionManager.clear();
    }
}
