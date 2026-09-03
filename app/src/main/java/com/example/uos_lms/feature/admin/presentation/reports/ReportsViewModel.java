package com.example.uos_lms.feature.admin.presentation.reports;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.ui.ChartEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ReportsViewModel extends ViewModel {

    private final MutableLiveData<ReportsUiState> uiState = new MutableLiveData<>(ReportsUiState.initial());

    private List<User> latestUsers;
    private List<Department> latestDepartments;
    private List<Subject> latestSubjects;
    private List<Assignment> latestAssignments;
    private List<Quiz> latestQuizzes;

    private final Map<String, Integer> attendancePercentByDept = new LinkedHashMap<>();

    @Inject
    public ReportsViewModel(
            ApiUserDataSource userDataSource,
            ApiUniversityDataSource universityDataSource,
            ApiAttendanceDataSource attendanceDataSource,
            ApiAssignmentDataSource assignmentDataSource,
            ApiQuizDataSource quizDataSource) {

        userDataSource.allUsers().addOnSuccessListener(users -> {
            latestUsers = users;
            recompute();
        });
        assignmentDataSource.allAssignments().addOnSuccessListener(assignments -> {
            latestAssignments = assignments;
            recompute();
        });
        quizDataSource.allQuizzes().addOnSuccessListener(quizzes -> {
            latestQuizzes = quizzes;
            recompute();
        });
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            latestDepartments = departments;
            loadSubjectsAndAttendance(departments, universityDataSource, attendanceDataSource);
        });
    }

    private void loadSubjectsAndAttendance(List<Department> departments, ApiUniversityDataSource universityDataSource,
                                            ApiAttendanceDataSource attendanceDataSource) {
        List<Task<List<Subject>>> subjectTasks = new ArrayList<>();
        for (Department department : departments) {
            subjectTasks.add(universityDataSource.listSubjectsForDepartment(department.getId()));
        }
        Tasks.whenAllSuccess(subjectTasks).addOnSuccessListener(results -> {
            List<Subject> allSubjects = new ArrayList<>();
            for (Object result : results) {
                //noinspection unchecked
                allSubjects.addAll((List<Subject>) result);
            }
            latestSubjects = allSubjects;
            recompute();
        });

        List<Task<?>> attendanceTasks = new ArrayList<>();
        for (Department department : departments) {
            attendanceTasks.add(attendanceDataSource.forDepartment(department.getId()).addOnSuccessListener(records -> {
                int present = 0;
                for (AttendanceRecord record : records) {
                    if (record.getStatus() == AttendanceStatus.PRESENT) present++;
                }
                int percentage = records.isEmpty() ? 0 : (present * 100) / records.size();
                attendancePercentByDept.put(department.getName(), percentage);
            }));
        }
        Tasks.whenAllComplete(attendanceTasks).addOnSuccessListener(v -> recompute());
    }

    private void recompute() {
        if (latestUsers == null || latestDepartments == null || latestSubjects == null
                || latestAssignments == null || latestQuizzes == null) {
            return;
        }
        int students = 0;
        int teachers = 0;
        int hods = 0;
        for (User user : latestUsers) {
            if (user.getRole() == UserRole.STUDENT) students++;
            else if (user.getRole() == UserRole.TEACHER) teachers++;
            else if (user.getRole() == UserRole.HOD) hods++;
        }

        List<ChartEntry> departmentAttendance = new ArrayList<>();
        for (Department department : latestDepartments) {
            Integer percent = attendancePercentByDept.get(department.getName());
            if (percent != null) departmentAttendance.add(new ChartEntry(department.getName(), percent));
        }

        Map<String, Integer> loadCounts = new LinkedHashMap<>();
        for (Subject subject : latestSubjects) {
            if (subject.getTeacherName() != null) {
                loadCounts.merge(subject.getTeacherName(), 1, Integer::sum);
            }
        }
        List<ChartEntry> teacherLoad = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : loadCounts.entrySet()) {
            teacherLoad.add(new ChartEntry(entry.getKey(), entry.getValue()));
        }
        teacherLoad.sort(Comparator.comparingInt(ChartEntry::getValue).reversed());
        if (teacherLoad.size() > 5) teacherLoad = teacherLoad.subList(0, 5);

        uiState.setValue(ReportsUiState.builder()
                .totalStudents(students)
                .totalTeachers(teachers)
                .totalHods(hods)
                .totalDepartments(latestDepartments.size())
                .totalSubjects(latestSubjects.size())
                .totalAssignments(latestAssignments.size())
                .totalQuizzes(latestQuizzes.size())
                .departmentAttendance(departmentAttendance)
                .teacherLoad(teacherLoad)
                .loading(false)
                .build());
    }

    public LiveData<ReportsUiState> getUiState() {
        return uiState;
    }
}
