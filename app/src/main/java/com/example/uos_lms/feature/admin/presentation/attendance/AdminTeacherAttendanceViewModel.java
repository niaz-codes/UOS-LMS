package com.example.uos_lms.feature.admin.presentation.attendance;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiTeacherAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Task;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminTeacherAttendanceViewModel extends ViewModel {

    private final ApiTeacherAttendanceDataSource teacherAttendanceDataSource;
    private final ApiUserDataSource userDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<AdminTeacherAttendanceUiState> uiState =
            new MutableLiveData<>(AdminTeacherAttendanceUiState.initial());

    @Inject
    public AdminTeacherAttendanceViewModel(
            ApiTeacherAttendanceDataSource teacherAttendanceDataSource,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource) {
        this.teacherAttendanceDataSource = teacherAttendanceDataSource;
        this.userDataSource = userDataSource;
        this.universityDataSource = universityDataSource;
        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> uiState.setValue(uiState.getValue().toBuilder().departments(departments).build()));
        loadRecords();
    }

    public LiveData<AdminTeacherAttendanceUiState> getUiState() {
        return uiState;
    }

    public void onDepartmentSelected(@Nullable Department department) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedDepartment(department)
                .selectedTeacher(null)
                .teachers(Collections.emptyList())
                .selectedSemester(null)
                .semesters(Collections.emptyList())
                .build());

        if (department == null) {
            loadRecords();
            return;
        }

        userDataSource.listApprovedTeachersInDepartment(department.getId())
                .addOnSuccessListener(teachers -> uiState.setValue(uiState.getValue().toBuilder().teachers(teachers).build()));
        universityDataSource.listSemesters(department.getId())
                .addOnSuccessListener(semesters -> uiState.setValue(uiState.getValue().toBuilder().semesters(semesters).build()));
        loadRecords();
    }

    public void onTeacherSelected(@Nullable User teacher) {
        uiState.setValue(uiState.getValue().toBuilder().selectedTeacher(teacher).build());
        loadRecords();
    }

    public void onSemesterSelected(@Nullable Semester semester) {
        uiState.setValue(uiState.getValue().toBuilder().selectedSemester(semester).build());
    }

    private void loadRecords() {
        AdminTeacherAttendanceUiState current = uiState.getValue();
        uiState.setValue(current.toBuilder().loading(true).build());

        Task<List<TeacherAttendanceRecord>> task;
        if (current.getSelectedTeacher() != null) {
            task = teacherAttendanceDataSource.forTeacher(current.getSelectedTeacher().getUid());
        } else if (current.getSelectedDepartment() != null) {
            task = teacherAttendanceDataSource.forDepartment(current.getSelectedDepartment().getId());
        } else {
            task = teacherAttendanceDataSource.listAll();
        }

        task.addOnSuccessListener(records ->
                        uiState.setValue(uiState.getValue().toBuilder().records(records).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
