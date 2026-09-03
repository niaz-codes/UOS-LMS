package com.example.uos_lms.feature.hod.presentation.leave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiLeaveDataSource;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodLeaveViewModel extends ViewModel {

    private final ApiLeaveDataSource leaveDataSource;

    private final MutableLiveData<HodLeaveUiState> uiState = new MutableLiveData<>(HodLeaveUiState.initial());

    @Inject
    public HodLeaveViewModel(ApiLeaveDataSource leaveDataSource) {
        this.leaveDataSource = leaveDataSource;
        load();
    }

    private void load() {
        leaveDataSource.list("PENDING")
                .addOnSuccessListener(leaves -> uiState.setValue(uiState.getValue().toBuilder().pending(leaves).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
        leaveDataSource.all()
                .addOnSuccessListener(leaves -> uiState.setValue(uiState.getValue().toBuilder().all(leaves).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<HodLeaveUiState> getUiState() {
        return uiState;
    }

    public void selectTab(LeaveReviewTab tab) {
        uiState.setValue(uiState.getValue().toBuilder().tab(tab).build());
    }

    public void approve(LeaveApplication leave) {
        act(leave, true);
    }

    public void reject(LeaveApplication leave) {
        act(leave, false);
    }

    private void act(LeaveApplication leave, boolean approve) {
        uiState.setValue(uiState.getValue().toBuilder().processingLeaveId(leave.getId()).errorMessage(null).build());
        Task<LeaveApplication> task = approve ? leaveDataSource.approve(leave.getId()) : leaveDataSource.reject(leave.getId());
        task.addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingLeaveId(null)
                            .actionMessage((approve ? "Approved " : "Rejected ") + leave.getStudentName() + "'s leave request.")
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingLeaveId(null).errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
