package com.example.uos_lms.feature.student.presentation.timetable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;
import com.example.uos_lms.core.domain.model.DayOfWeek;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentTimetableViewModel extends ViewModel {

    private final MutableLiveData<StudentTimetableUiState> uiState = new MutableLiveData<>(StudentTimetableUiState.initial());

    @Inject
    public StudentTimetableViewModel(ApiSchedulingDataSource schedulingDataSource) {
        schedulingDataSource.mySlots()
                .addOnSuccessListener(slots -> uiState.setValue(uiState.getValue().toBuilder().allSlots(slots).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<StudentTimetableUiState> getUiState() {
        return uiState;
    }

    public void selectDay(DayOfWeek day) {
        uiState.setValue(uiState.getValue().toBuilder().selectedDay(day).build());
    }
}
