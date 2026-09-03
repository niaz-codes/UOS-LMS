package com.example.uos_lms.feature.admin.presentation.examresult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.domain.model.ResultStatus;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminExamResultMonitorViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;

    private final MediatorLiveData<AdminExamResultUiState> uiState =
            new MediatorLiveData<>(AdminExamResultUiState.initial());

    @Inject
    public AdminExamResultMonitorViewModel(ApiExamResultDataSource examResultDataSource) {
        this.examResultDataSource = examResultDataSource;
        load();
    }

    public LiveData<AdminExamResultUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        examResultDataSource.listForCurrentRole()
                .addOnSuccessListener(results -> uiState.setValue(uiState.getValue().toBuilder().results(results).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public void setStatusFilter(ResultStatus status) {
        uiState.setValue(uiState.getValue().toBuilder().statusFilter(status).build());
    }
}
