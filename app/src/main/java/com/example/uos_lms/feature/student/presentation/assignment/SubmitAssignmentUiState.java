package com.example.uos_lms.feature.student.presentation.assignment;

import android.net.Uri;

import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SubmitAssignmentUiState {
    private final Assignment assignment;
    private final AssignmentSubmission existingSubmission;
    @Builder.Default
    private final String textAnswer = "";
    private final Uri pickedFileUri;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean submitting = false;
    private final Integer uploadProgress;
    private final String errorMessage;

    public static SubmitAssignmentUiState initial() {
        return SubmitAssignmentUiState.builder().build();
    }
}
