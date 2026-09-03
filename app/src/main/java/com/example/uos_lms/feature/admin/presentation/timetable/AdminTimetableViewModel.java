package com.example.uos_lms.feature.admin.presentation.timetable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TimetableSlot;

import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminTimetableViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiSchedulingDataSource schedulingDataSource;

    private final MutableLiveData<AdminTimetableUiState> uiState = new MutableLiveData<>(AdminTimetableUiState.initial());

    @Inject
    public AdminTimetableViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiSchedulingDataSource schedulingDataSource) {
        this.universityDataSource = universityDataSource;
        this.schedulingDataSource = schedulingDataSource;
        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> uiState.setValue(uiState.getValue().toBuilder().departments(departments).build()));
    }

    public LiveData<AdminTimetableUiState> getUiState() {
        return uiState;
    }

    public void selectDepartment(Department department) {
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedDepartment(department)
                .selectedSemester(null)
                .allSlots(Collections.emptyList())
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

        loadSlots(department.getId(), semester.getId());
    }

    private void loadSlots(String departmentId, String semesterId) {
        schedulingDataSource.slotsForDepartmentSemester(departmentId, semesterId)
                .addOnSuccessListener(slots -> uiState.setValue(uiState.getValue().toBuilder().allSlots(slots).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void selectDay(DayOfWeek day) {
        uiState.setValue(uiState.getValue().toBuilder().selectedDay(day).build());
    }

    public void addSlot(Subject subject, DayOfWeek day, int startMinutes, int endMinutes, String room) {
        AdminTimetableUiState state = uiState.getValue();
        Department department = state.getSelectedDepartment();
        Semester semester = state.getSelectedSemester();
        if (department == null || semester == null) return;

        uiState.setValue(uiState.getValue().toBuilder().saving(true).errorMessage(null).build());
        schedulingDataSource.createSlot(subject.getId(), day, startMinutes, endMinutes, room)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).actionMessage("Class added to the timetable.").build());
                    loadSlots(department.getId(), semester.getId());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .saving(false).errorMessage(e.getMessage()).build()));
    }

    public void deleteSlot(TimetableSlot slot) {
        AdminTimetableUiState state = uiState.getValue();
        Department department = state.getSelectedDepartment();
        Semester semester = state.getSelectedSemester();
        schedulingDataSource.deleteSlot(slot.getId())
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().actionMessage("Slot removed.").build());
                    if (department != null && semester != null) loadSlots(department.getId(), semester.getId());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
