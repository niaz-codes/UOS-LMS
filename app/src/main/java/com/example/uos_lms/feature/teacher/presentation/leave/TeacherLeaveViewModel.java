package com.example.uos_lms.feature.teacher.presentation.leave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiLeaveDataSource;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherLeaveViewModel extends ViewModel {

    private final ApiLeaveDataSource leaveDataSource;

    private final MutableLiveData<TeacherLeaveUiState> uiState = new MutableLiveData<>(TeacherLeaveUiState.initial());

    @Inject
    public TeacherLeaveViewModel(ApiLeaveDataSource leaveDataSource) {
        this.leaveDataSource = leaveDataSource;
        load();
    }

    private void load() {
        // The backend already scopes this to every department the Teacher has joined
        // (User.departmentIds) in one query - see leaveController.list.
        leaveDataSource.list("PENDING")
                .addOnSuccessListener(leaves -> {
                    List<LeaveApplication> sorted = new ArrayList<>(leaves);
                    sorted.sort((a, b) -> a.getStudentName().compareToIgnoreCase(b.getStudentName()));
                    uiState.setValue(uiState.getValue().toBuilder().pending(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<TeacherLeaveUiState> getUiState() {
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
