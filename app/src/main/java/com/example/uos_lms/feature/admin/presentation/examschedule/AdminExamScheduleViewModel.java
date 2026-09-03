package com.example.uos_lms.feature.admin.presentation.examschedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.ExamSchedule;
import com.example.uos_lms.core.domain.model.ExamType;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminExamScheduleViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiSchedulingDataSource schedulingDataSource;

    private final MutableLiveData<AdminExamScheduleUiState> uiState = new MutableLiveData<>(AdminExamScheduleUiState.initial());

    @Inject
    public AdminExamScheduleViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiSchedulingDataSource schedulingDataSource,
            ApiUserDataSource userDataSource) {
        this.universityDataSource = universityDataSource;
        this.schedulingDataSource = schedulingDataSource;
        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> uiState.setValue(uiState.getValue().toBuilder().departments(departments).build()));
        userDataSource.listApprovedTeachers()
                .addOnSuccessListener(teachers -> uiState.setValue(uiState.getValue().toBuilder().teachers(teachers).build()));
    }

    public LiveData<AdminExamScheduleUiState> getUiState() {
        return uiState;
    }

    public void selectDepartment(Department department) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedDepartment(department)
                .selectedSemester(null)
                .schedules(Collections.emptyList())
                .build());
        universityDataSource.listSemesters(department.getId())
                .addOnSuccessListener(semesters -> uiState.setValue(uiState.getValue().toBuilder().semesters(semesters).build()));
    }

    public void selectSemester(Semester semester) {
        Department department = uiState.getValue().getSelectedDepartment();
        if (department == null) return;
        uiState.setValue(uiState.getValue().toBuilder().selectedSemester(semester).build());

        universityDataSource.listSubjectsForSemester(semester.getId())
                .addOnSuccessListener(subjects -> uiState.setValue(uiState.getValue().toBuilder().subjects(subjects).build()));

        loadSchedules(department.getId(), semester.getId());
    }

    private void loadSchedules(String departmentId, String semesterId) {
        schedulingDataSource.examSchedulesForDepartment(departmentId)
                .addOnSuccessListener(schedules -> {
                    java.util.List<ExamSchedule> forSemester = new java.util.ArrayList<>();
                    for (ExamSchedule schedule : schedules) {
                        if (schedule.getSemesterId().equals(semesterId)) forSemester.add(schedule);
                    }
                    uiState.setValue(uiState.getValue().toBuilder().schedules(forSemester).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void addSchedule(Subject subject, ExamType examType, long examDateMillis, int startMinutes, int endMinutes,
                             String room, User invigilator) {
        AdminExamScheduleUiState state = uiState.getValue();
        Department department = state.getSelectedDepartment();
        Semester semester = state.getSelectedSemester();
        if (department == null || semester == null) return;

        uiState.setValue(uiState.getValue().toBuilder().saving(true).errorMessage(null).build());
        schedulingDataSource.createExamSchedule(subject.getId(), examType, examDateMillis, startMinutes, endMinutes,
                        room, invigilator != null ? invigilator.getUid() : null)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage("Exam scheduled.").build());
                    loadSchedules(department.getId(), semester.getId());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .saving(false).errorMessage(e.getMessage()).build()));
    }

    public void publish(ExamSchedule schedule) {
        schedulingDataSource.publishExamSchedule(schedule.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Exam published.").build());
                    reloadCurrentSelection();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void lock(ExamSchedule schedule) {
        schedulingDataSource.lockExamSchedule(schedule.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Exam schedule locked.").build());
                    reloadCurrentSelection();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void deleteSchedule(ExamSchedule schedule) {
        schedulingDataSource.deleteExamSchedule(schedule.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Exam schedule removed.").build());
                    reloadCurrentSelection();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    private void reloadCurrentSelection() {
        Department department = uiState.getValue().getSelectedDepartment();
        Semester semester = uiState.getValue().getSelectedSemester();
        if (department != null && semester != null) loadSchedules(department.getId(), semester.getId());
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
