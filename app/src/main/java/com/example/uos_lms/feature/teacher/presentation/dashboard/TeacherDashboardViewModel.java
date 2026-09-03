package com.example.uos_lms.feature.teacher.presentation.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherDashboardViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiAssignmentDataSource assignmentDataSource;
    private final AuthApi authApi;
    private final SessionManager sessionManager;

    private final MutableLiveData<TeacherDashboardUiState> uiState =
            new MutableLiveData<>(TeacherDashboardUiState.initial());

    private List<Subject> latestSubjects;
    private final Map<String, List<Semester>> semestersByDept = new HashMap<>();
    private final Map<String, String> departmentNamesById = new HashMap<>();
    private final Map<String, String> sessionLabelsById = new HashMap<>();
    private final Map<String, Set<String>> subjectSessionIdsById = new HashMap<>();
    private final Map<String, List<User>> studentsBySubject = new HashMap<>();
    private final Map<String, List<AttendanceRecord>> attendanceBySubject = new HashMap<>();
    private final Map<String, List<AssignmentSubmission>> submissionsBySubject = new HashMap<>();

    @Inject
    public TeacherDashboardViewModel(
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

    public LiveData<TeacherDashboardUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches everything on this dashboard from the backend without blanking the currently-
     * shown stats/subjects - no-ops while a refresh is already in flight. */
    public void refresh() {
        TeacherDashboardUiState current = uiState.getValue();
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
                    uiState.setValue(uiState.getValue().toBuilder().fullName(user.getFullName()).build());

                    universityDataSource.listSubjectsForTeacher("me").addOnSuccessListener(subjects -> {
                        latestSubjects = subjects;
                        loadSubjectMetadata(subjects);
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    /** Fetches department names, each department's semesters and sessions, and the
     * enrolled students' sessions per subject. All of it is scoped to just the
     * departments/semesters this teacher's subjects actually span (not the whole
     * university), and every task is awaited together so nothing renders partially. */
    private void loadSubjectMetadata(List<Subject> subjects) {
        Set<String> departmentIds = new HashSet<>();
        for (Subject subject : subjects) departmentIds.add(subject.getDepartmentId());

        if (departmentIds.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
            return;
        }

        List<Task<?>> tasks = new ArrayList<>();

        tasks.add(universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (departmentIds.contains(department.getId())) departmentNamesById.put(department.getId(), department.getName());
            }
        }));

        for (String departmentId : departmentIds) {
            tasks.add(universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> semestersByDept.put(departmentId, semesters)));
            tasks.add(universityDataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
                for (Session session : sessions) sessionLabelsById.put(session.getId(), session.getLabel());
            }));
        }

        for (Subject subject : subjects) {
            String subjectId = subject.getId();
            tasks.add(universityDataSource.subjectRoster(subjectId).addOnSuccessListener(students -> {
                Set<String> sessionIds = new HashSet<>();
                for (User student : students) {
                    if (student.getSessionId() != null && !student.getSessionId().isEmpty()) sessionIds.add(student.getSessionId());
                }
                subjectSessionIdsById.put(subjectId, sessionIds);
                studentsBySubject.put(subjectId, students);
            }));
            tasks.add(attendanceDataSource.historyForSubject(subjectId).addOnSuccessListener(records ->
                    attendanceBySubject.put(subjectId, records)));
            tasks.add(assignmentDataSource.submissionsForSubject(subjectId).addOnSuccessListener(submissions ->
                    submissionsBySubject.put(subjectId, submissions)));
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recombine());
    }

    private void recombine() {
        if (latestSubjects == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
            return;
        }

        List<AssignedSubject> assigned = new ArrayList<>();
        for (Subject subject : latestSubjects) {
            List<Semester> deptSemesters = semestersByDept.get(subject.getDepartmentId());
            Semester semester = null;
            if (deptSemesters != null) {
                for (Semester candidate : deptSemesters) {
                    if (candidate.getId().equals(subject.getSemesterId())) {
                        semester = candidate;
                        break;
                    }
                }
            }
            String departmentName = departmentNamesById.getOrDefault(subject.getDepartmentId(), "");
            assigned.add(AssignedSubject.builder()
                    .subject(subject)
                    .departmentName(departmentName.isEmpty() ? "Department —" : "Department: " + departmentName)
                    .semesterNumber(semester != null ? semester.getNumber() : 0)
                    .semesterLabel(semester != null ? semester.getDisplayName() : "Semester —")
                    .sessionLabel(buildSessionLabel(subjectSessionIdsById.get(subject.getId())))
                    .build());
        }
        assigned.sort((a, b) -> {
            int cmp = Integer.compare(a.getSemesterNumber(), b.getSemesterNumber());
            return cmp != 0 ? cmp : a.getSubject().getCode().compareTo(b.getSubject().getCode());
        });

        Map<String, User> distinctStudents = new LinkedHashMap<>();
        for (List<User> students : studentsBySubject.values()) {
            for (User student : students) distinctStudents.put(student.getUid(), student);
        }

        int totalSubmissions = 0;
        for (List<AssignmentSubmission> submissions : submissionsBySubject.values()) totalSubmissions += submissions.size();

        int presentCount = 0;
        int totalRecords = 0;
        for (List<AttendanceRecord> records : attendanceBySubject.values()) {
            totalRecords += records.size();
            for (AttendanceRecord record : records) {
                if (record.getStatus() == AttendanceStatus.PRESENT) presentCount++;
            }
        }
        int attendancePercentage = totalRecords == 0 ? 0 : (presentCount * 100) / totalRecords;

        uiState.setValue(uiState.getValue().toBuilder()
                .subjects(assigned)
                .totalStudents(distinctStudents.size())
                .totalSubmissions(totalSubmissions)
                .attendancePercentage(attendancePercentage)
                .loading(false)
                .refreshing(false)
                .build());
    }

    /** The sessions of the students actually enrolled in a subject (a subject is shared
     * across every cohort/session of its department+semester, so multiple sessions may
     * appear). Sorted and comma-separated; "Session —" if none are enrolled yet. */
    private String buildSessionLabel(Set<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return "Session —";
        List<String> labels = new ArrayList<>();
        for (String sessionId : sessionIds) {
            String label = sessionLabelsById.get(sessionId);
            if (label != null && !label.isEmpty()) labels.add(label);
        }
        if (labels.isEmpty()) return "Session —";
        labels.sort(String::compareTo);
        return "Session: " + String.join(", ", labels);
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }

    public void logout() {
        sessionManager.clear();
    }
}
