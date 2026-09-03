package com.example.uos_lms.feature.student.presentation.examschedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentExamScheduleViewModel extends ViewModel {

    private final MutableLiveData<StudentExamScheduleUiState> uiState = new MutableLiveData<>(StudentExamScheduleUiState.initial());

    @Inject
    public StudentExamScheduleViewModel(ApiSchedulingDataSource schedulingDataSource) {
        schedulingDataSource.myExamSchedules()
                .addOnSuccessListener(schedules -> uiState.setValue(uiState.getValue().toBuilder().schedules(schedules).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<StudentExamScheduleUiState> getUiState() {
        return uiState;
    }
}
