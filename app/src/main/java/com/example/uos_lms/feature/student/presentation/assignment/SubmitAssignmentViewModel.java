package com.example.uos_lms.feature.student.presentation.assignment;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiMediaDataSource;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.example.uos_lms.core.domain.model.Assignment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SubmitAssignmentViewModel extends ViewModel {

    private static final String MEDIA_CATEGORY = "assignment_submission";

    private final ApiAssignmentDataSource assignmentDataSource;
    private final ApiMediaDataSource mediaDataSource;
    private final String assignmentId;
    private final String subjectId;

    private final MutableLiveData<SubmitAssignmentUiState> uiState = new MutableLiveData<>(SubmitAssignmentUiState.initial());
    private final MutableLiveData<Boolean> submitted = new MutableLiveData<>(false);

    @Inject
    public SubmitAssignmentViewModel(
            SavedStateHandle savedStateHandle,
            ApiAssignmentDataSource assignmentDataSource,
            ApiMediaDataSource mediaDataSource) {
        this.assignmentDataSource = assignmentDataSource;
        this.mediaDataSource = mediaDataSource;
        this.assignmentId = savedStateHandle.get("assignmentId");
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    private void load() {
        assignmentDataSource.assignmentsForSubject(subjectId)
                .addOnSuccessListener(assignments -> {
                    Assignment match = null;
                    for (Assignment assignment : assignments) {
                        if (assignment.getId().equals(assignmentId)) {
                            match = assignment;
                            break;
                        }
                    }
                    uiState.setValue(uiState.getValue().toBuilder().assignment(match).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));

        assignmentDataSource.mySubmissionForAssignment(assignmentId)
                .addOnSuccessListener(submission -> {
                    SubmitAssignmentUiState current = uiState.getValue();
                    String textAnswer = submission != null && submission.getTextAnswer() != null
                            ? submission.getTextAnswer() : current.getTextAnswer();
                    uiState.setValue(current.toBuilder().existingSubmission(submission).textAnswer(textAnswer).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<SubmitAssignmentUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getSubmitted() {
        return submitted;
    }

    public void onTextAnswerChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().textAnswer(value).build());
    }

    public void onFilePicked(Uri uri) {
        uiState.setValue(uiState.getValue().toBuilder().pickedFileUri(uri).build());
    }

    public void submit() {
        SubmitAssignmentUiState state = uiState.getValue();
        String textAnswer = state.getTextAnswer();

        uiState.setValue(state.toBuilder().submitting(true).uploadProgress(null).errorMessage(null).build());

        Task<MediaResponseDto> uploadTask = state.getPickedFileUri() != null
                ? mediaDataSource.upload(MEDIA_CATEGORY, "assignment", assignmentId, state.getPickedFileUri(),
                percent -> uiState.postValue(uiState.getValue().toBuilder().uploadProgress(percent).build()))
                : Tasks.forResult(null);

        uploadTask.continueWithTask(uploadResultTask -> {
            MediaResponseDto media = uploadResultTask.getResult();
            String mediaId = media != null ? media.getId() : null;
            return assignmentDataSource.submitAssignment(assignmentId, textAnswer, mediaId);
        }).addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .submitting(false).uploadProgress(null).pickedFileUri(null).build());
                    submitted.setValue(true);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .submitting(false).uploadProgress(null).errorMessage(e.getMessage()).build()));
    }
}
