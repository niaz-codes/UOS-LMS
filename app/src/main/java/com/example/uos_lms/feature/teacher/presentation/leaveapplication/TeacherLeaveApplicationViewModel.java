package com.example.uos_lms.feature.teacher.presentation.leaveapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiTeacherLeaveDataSource;
import com.example.uos_lms.core.domain.model.LeaveType;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherLeaveApplicationViewModel extends ViewModel {

    private final ApiTeacherLeaveDataSource leaveDataSource;

    private final MutableLiveData<TeacherLeaveApplicationUiState> uiState =
            new MutableLiveData<>(TeacherLeaveApplicationUiState.initial());

    @Inject
    public TeacherLeaveApplicationViewModel(ApiTeacherLeaveDataSource leaveDataSource) {
        this.leaveDataSource = leaveDataSource;
        load();
    }

    private void load() {
        leaveDataSource.list(null)
                .addOnSuccessListener(leaves -> {
                    List<TeacherLeaveApplication> sorted = new ArrayList<>(leaves);
                    sorted.sort(Comparator.comparingLong(TeacherLeaveApplication::getCreatedAt).reversed());
                    uiState.setValue(uiState.getValue().toBuilder().leaves(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<TeacherLeaveApplicationUiState> getUiState() {
        return uiState;
    }

    public void applyForLeave(LeaveType leaveType, long fromDateMillis, long toDateMillis, String reason) {
        uiState.setValue(uiState.getValue().toBuilder().applying(true).errorMessage(null).build());
        leaveDataSource.applyForLeave(leaveType, fromDateMillis, toDateMillis, reason)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().applying(false).actionMessage("Leave application submitted.").build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .applying(false).errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
