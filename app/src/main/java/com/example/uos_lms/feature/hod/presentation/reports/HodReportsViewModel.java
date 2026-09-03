package com.example.uos_lms.feature.hod.presentation.reports;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.ui.ChartEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodReportsViewModel extends ViewModel {

    private final MutableLiveData<HodReportsUiState> uiState = new MutableLiveData<>(HodReportsUiState.initial());

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final ApiAttendanceDataSource attendanceDataSource;
    private final AuthApi authApi;

    private String departmentId;
    private List<User> latestUsers;
    private List<Subject> latestSubjects;
    private List<Semester> latestSemesters;

    private final Map<String, Integer> semesterPercentages = new HashMap<>();

    @Inject
    public HodReportsViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            ApiAttendanceDataSource attendanceDataSource,
            AuthApi authApi) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.attendanceDataSource = attendanceDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<HodReportsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches every figure on this report from the backend without blanking the currently-
     * shown charts/stats - no-ops while a refresh is already in flight. */
    public void refresh() {
        HodReportsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    departmentId = user != null ? user.getDepartment() : null;
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    userDataSource.allUsers().addOnSuccessListener(users -> {
                        latestUsers = users;
                        recompute();
                    });
                    universityDataSource.listSubjectsForDepartment(departmentId).addOnSuccessListener(subjects -> {
                        latestSubjects = subjects;
                        recompute();
                    });
                    universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
                        List<Semester> sorted = new ArrayList<>(semesters);
                        sorted.sort(Comparator.comparingInt(Semester::getNumber));
                        latestSemesters = sorted;
                        loadAttendance(sorted);
                    });
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void loadAttendance(List<Semester> semesters) {
        List<Task<?>> tasks = new ArrayList<>();
        for (Semester semester : semesters) {
            tasks.add(attendanceDataSource.forSemester(departmentId, semester.getId()).addOnSuccessListener(records -> {
                int present = 0;
                for (AttendanceRecord record : records) {
                    if (record.getStatus() == AttendanceStatus.PRESENT) present++;
                }
                int percentage = records.isEmpty() ? 0 : (present * 100) / records.size();
                semesterPercentages.put(semester.getId(), percentage);
            }));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
    }

    private void recompute() {
        if (latestUsers == null || latestSubjects == null || latestSemesters == null) return;

        int totalTeachers = 0;
        int totalStudents = 0;
        for (User user : latestUsers) {
            if (user.getRole() == UserRole.TEACHER) totalTeachers++;
            else if (user.getRole() == UserRole.STUDENT) totalStudents++;
        }

        List<Subject> subjectsInDept = latestSubjects;

        List<ChartEntry> semesterAttendance = new ArrayList<>();
        for (Semester semester : latestSemesters) {
            Integer percentage = semesterPercentages.get(semester.getId());
            if (percentage != null) semesterAttendance.add(new ChartEntry(semester.getDisplayName(), percentage));
        }

        Map<String, Integer> loadCounts = new LinkedHashMap<>();
        for (Subject subject : subjectsInDept) {
            if (subject.getTeacherName() == null) continue;
            loadCounts.merge(subject.getTeacherName(), 1, Integer::sum);
        }
        List<ChartEntry> teacherLoad = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : loadCounts.entrySet()) {
            teacherLoad.add(new ChartEntry(entry.getKey(), entry.getValue()));
        }
        teacherLoad.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (teacherLoad.size() > 5) teacherLoad = teacherLoad.subList(0, 5);

        uiState.setValue(uiState.getValue().toBuilder()
                .totalTeachers(totalTeachers)
                .totalStudents(totalStudents)
                .totalSubjects(subjectsInDept.size())
                .totalSemesters(latestSemesters.size())
                .semesterAttendance(semesterAttendance)
                .teacherLoad(teacherLoad)
                .loading(false)
                .refreshing(false)
                .build());
    }
}
