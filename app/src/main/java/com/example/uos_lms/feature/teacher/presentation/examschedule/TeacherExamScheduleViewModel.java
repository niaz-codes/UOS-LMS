package com.example.uos_lms.feature.teacher.presentation.examschedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiSchedulingDataSource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherExamScheduleViewModel extends ViewModel {

    private final MutableLiveData<TeacherExamScheduleUiState> uiState = new MutableLiveData<>(TeacherExamScheduleUiState.initial());

    @Inject
    public TeacherExamScheduleViewModel(ApiSchedulingDataSource schedulingDataSource) {
        schedulingDataSource.myExamSchedules()
                .addOnSuccessListener(schedules -> uiState.setValue(uiState.getValue().toBuilder().schedules(schedules).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<TeacherExamScheduleUiState> getUiState() {
        return uiState;
    }
}
