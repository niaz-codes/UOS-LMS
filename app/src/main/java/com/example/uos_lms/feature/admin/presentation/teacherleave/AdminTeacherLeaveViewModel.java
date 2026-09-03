package com.example.uos_lms.feature.admin.presentation.teacherleave;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiTeacherLeaveDataSource;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Admin reviews Teacher leave applications across every department - the backend
 * teacherLeaveController.list returns everything for an Admin, and decide() allows an Admin
 * on any department's request. Mirrors HodTeacherLeaveViewModel, which is department-scoped. */
@HiltViewModel
public class AdminTeacherLeaveViewModel extends ViewModel {

    private final ApiTeacherLeaveDataSource leaveDataSource;

    private final MutableLiveData<AdminTeacherLeaveUiState> uiState = new MutableLiveData<>(AdminTeacherLeaveUiState.initial());

    @Inject
    public AdminTeacherLeaveViewModel(ApiTeacherLeaveDataSource leaveDataSource) {
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

    public LiveData<AdminTeacherLeaveUiState> getUiState() {
        return uiState;
    }

    public void selectTab(AdminTeacherLeaveReviewTab tab) {
        uiState.setValue(uiState.getValue().toBuilder().tab(tab).build());
    }

    public void approve(TeacherLeaveApplication leave) {
        uiState.setValue(uiState.getValue().toBuilder().processingLeaveId(leave.getId()).errorMessage(null).build());
        act(leave, leaveDataSource.approve(leave.getId()), true);
    }

    public void reject(TeacherLeaveApplication leave, String reason) {
        uiState.setValue(uiState.getValue().toBuilder().processingLeaveId(leave.getId()).errorMessage(null).build());
        act(leave, leaveDataSource.reject(leave.getId(), reason), false);
    }

    private void act(TeacherLeaveApplication leave, Task<TeacherLeaveApplication> task, boolean approve) {
        task.addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingLeaveId(null)
                            .actionMessage((approve ? "Approved " : "Rejected ") + leave.getTeacherName() + "'s leave request.")
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
