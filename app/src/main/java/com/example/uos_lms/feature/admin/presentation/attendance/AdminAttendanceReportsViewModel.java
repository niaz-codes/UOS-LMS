package com.example.uos_lms.feature.admin.presentation.attendance;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAttendanceReportsViewModel extends ViewModel {

    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<AdminAttendanceReportsUiState> uiState =
            new MutableLiveData<>(AdminAttendanceReportsUiState.initial());

    @Inject
    public AdminAttendanceReportsViewModel(
            ApiAttendanceDataSource attendanceDataSource,
            ApiUniversityDataSource universityDataSource) {
        this.attendanceDataSource = attendanceDataSource;
        this.universityDataSource = universityDataSource;
        load();
    }

    public LiveData<AdminAttendanceReportsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches departments and (if a department/semester is already selected) the currently
     * filtered records, without blanking anything shown - no-ops while a refresh is already in
     * flight. */
    public void refresh() {
        AdminAttendanceReportsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
        Department department = current.getSelectedDepartment();
        if (department != null) {
            Semester semester = current.getSelectedSemester();
            loadRecords(department.getId(), semester != null ? semester.getId() : null);
        }
    }

    private void load() {
        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> uiState.setValue(uiState.getValue().toBuilder().departments(departments).loading(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public void onDepartmentSelected(@Nullable Department department) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedDepartment(department)
                .selectedSemester(null)
                .semesters(Collections.emptyList())
                .records(Collections.emptyList())
                .build());

        if (department == null) return;

        universityDataSource.listSemesters(department.getId())
                .addOnSuccessListener(semesters -> {
                    List<Semester> sorted = new ArrayList<>(semesters);
                    Collections.sort(sorted, Comparator.comparingInt(Semester::getNumber));
                    uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).build());
                });
        loadRecords(department.getId(), null);
    }

    public void onSemesterSelected(@Nullable Semester semester) {
        Department department = uiState.getValue().getSelectedDepartment();
        if (department == null) return;
        uiState.setValue(uiState.getValue().toBuilder().selectedSemester(semester).build());
        loadRecords(department.getId(), semester != null ? semester.getId() : null);
    }

    private void loadRecords(String departmentId, @Nullable String semesterId) {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).build());
        Task<List<AttendanceRecord>> task = semesterId != null
                ? attendanceDataSource.forSemester(departmentId, semesterId)
                : attendanceDataSource.forDepartment(departmentId);
        task.addOnSuccessListener(records ->
                        uiState.setValue(uiState.getValue().toBuilder().records(records).loading(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public String buildCsv() {
        StringBuilder sb = new StringBuilder("Student Name,Subject ID,Date,Status");
        for (AttendanceRecord record : uiState.getValue().getRecords()) {
            sb.append('\n')
                    .append(record.getStudentName()).append(',')
                    .append(record.getSubjectId()).append(',')
                    .append(record.getDateKey()).append(',')
                    .append(record.getStatus().name());
        }
        return sb.toString();
    }
}
