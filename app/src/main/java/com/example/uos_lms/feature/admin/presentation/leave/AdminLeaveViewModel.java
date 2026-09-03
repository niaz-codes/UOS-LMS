package com.example.uos_lms.feature.admin.presentation.leave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiLeaveDataSource;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminLeaveViewModel extends ViewModel {

    private final ApiLeaveDataSource leaveDataSource;

    private final MutableLiveData<AdminLeaveUiState> uiState = new MutableLiveData<>(AdminLeaveUiState.initial());

    @Inject
    public AdminLeaveViewModel(ApiLeaveDataSource leaveDataSource) {
        this.leaveDataSource = leaveDataSource;
        load();
    }

    private void load() {
        leaveDataSource.all()
                .addOnSuccessListener(leaves -> uiState.setValue(uiState.getValue().toBuilder().leaves(leaves).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<AdminLeaveUiState> getUiState() {
        return uiState;
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
