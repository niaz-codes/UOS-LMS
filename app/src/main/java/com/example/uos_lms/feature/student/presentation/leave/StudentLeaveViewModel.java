package com.example.uos_lms.feature.student.presentation.leave;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiLeaveDataSource;
import com.example.uos_lms.core.data.remote.api.ApiMediaDataSource;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentLeaveViewModel extends ViewModel {

    private static final String MEDIA_CATEGORY = "leave_attachment";

    private final ApiLeaveDataSource leaveDataSource;
    private final ApiMediaDataSource mediaDataSource;

    private final MutableLiveData<StudentLeaveUiState> uiState = new MutableLiveData<>(StudentLeaveUiState.initial());

    @Inject
    public StudentLeaveViewModel(ApiLeaveDataSource leaveDataSource, ApiMediaDataSource mediaDataSource) {
        this.leaveDataSource = leaveDataSource;
        this.mediaDataSource = mediaDataSource;
        load();
    }

    private void load() {
        leaveDataSource.list(null)
                .addOnSuccessListener(leaves -> {
                    List<LeaveApplication> sorted = new ArrayList<>(leaves);
                    sorted.sort(Comparator.comparingLong(LeaveApplication::getCreatedAt).reversed());
                    uiState.setValue(uiState.getValue().toBuilder().leaves(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<StudentLeaveUiState> getUiState() {
        return uiState;
    }

    public void applyForLeave(long fromDateMillis, long toDateMillis, String reason, @Nullable Uri attachmentUri) {
        uiState.setValue(uiState.getValue().toBuilder().applying(true).errorMessage(null).build());

        Task<MediaResponseDto> uploadTask = attachmentUri != null
                ? mediaDataSource.upload(MEDIA_CATEGORY, null, null, attachmentUri, null)
                : Tasks.forResult(null);

        uploadTask.continueWithTask(task -> {
            MediaResponseDto media = task.getResult();
            String mediaId = media != null ? media.getId() : null;
            return leaveDataSource.applyForLeave(fromDateMillis, toDateMillis, reason, mediaId);
        })
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
