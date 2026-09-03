package com.example.uos_lms.feature.teacher.presentation.assignment;

import android.net.Uri;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreateAssignmentUiState {
    @Builder.Default
    private final String title = "";
    @Builder.Default
    private final String description = "";
    private final Long dueDateMillis;
    @Builder.Default
    private final String maxMarksText = "";
    private final Uri pickedFileUri;
    @Builder.Default
    private final boolean saving = false;
    private final Integer uploadProgress;
    private final String errorMessage;

    public static CreateAssignmentUiState initial() {
        return CreateAssignmentUiState.builder().build();
    }
}
