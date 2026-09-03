package com.example.uos_lms.feature.teacher.presentation.assignment;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiMediaDataSource;
import com.example.uos_lms.core.data.remote.api.dto.MediaResponseDto;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CreateAssignmentViewModel extends ViewModel {

    private static final String MEDIA_CATEGORY = "assignment";

    private final ApiAssignmentDataSource assignmentDataSource;
    private final ApiMediaDataSource mediaDataSource;
    private final SessionManager sessionManager;
    private final String subjectId;

    private final MutableLiveData<CreateAssignmentUiState> uiState = new MutableLiveData<>(CreateAssignmentUiState.initial());
    private final MutableLiveData<Boolean> created = new MutableLiveData<>(false);

    @Inject
    public CreateAssignmentViewModel(
            SavedStateHandle savedStateHandle,
            ApiAssignmentDataSource assignmentDataSource,
            ApiMediaDataSource mediaDataSource,
            SessionManager sessionManager) {
        this.assignmentDataSource = assignmentDataSource;
        this.mediaDataSource = mediaDataSource;
        this.sessionManager = sessionManager;
        this.subjectId = savedStateHandle.get("subjectId");
    }

    public LiveData<CreateAssignmentUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getCreated() {
        return created;
    }

    public void onTitleChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().title(value).errorMessage(null).build());
    }

    public void onDescriptionChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().description(value).build());
    }

    public void onDueDateChange(long millis) {
        uiState.setValue(uiState.getValue().toBuilder().dueDateMillis(millis).errorMessage(null).build());
    }

    public void onMaxMarksChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().maxMarksText(value).errorMessage(null).build());
    }

    public void onFilePicked(Uri uri) {
        uiState.setValue(uiState.getValue().toBuilder().pickedFileUri(uri).build());
    }

    public void save() {
        CreateAssignmentUiState state = uiState.getValue();
        Long dueDateMillis = state.getDueDateMillis();
        Integer maxMarks = null;
        try {
            maxMarks = Integer.parseInt(state.getMaxMarksText().trim());
        } catch (NumberFormatException ignored) {
            // handled below via null check
        }
        if (state.getTitle().isBlank()) {
            uiState.setValue(state.toBuilder().errorMessage("Title is required").build());
            return;
        }
        if (dueDateMillis == null) {
            uiState.setValue(state.toBuilder().errorMessage("Pick a due date").build());
            return;
        }
        if (maxMarks == null || maxMarks <= 0) {
            uiState.setValue(state.toBuilder().errorMessage("Enter valid max marks").build());
            return;
        }
        CachedSession session = sessionManager.getCachedSession().getValue();
        if (session == null) {
            uiState.setValue(state.toBuilder().errorMessage("Session expired. Please log in again.").build());
            return;
        }

        String title = state.getTitle().trim();
        String description = state.getDescription().trim();
        long finalDueDateMillis = dueDateMillis;
        int finalMaxMarks = maxMarks;

        uiState.setValue(state.toBuilder().saving(true).uploadProgress(null).errorMessage(null).build());

        Task<MediaResponseDto> uploadTask = state.getPickedFileUri() != null
                ? mediaDataSource.upload(MEDIA_CATEGORY, "subject", subjectId, state.getPickedFileUri(),
                percent -> uiState.postValue(uiState.getValue().toBuilder().uploadProgress(percent).build()))
                : Tasks.forResult(null);

        uploadTask.continueWithTask(uploadResultTask -> {
            MediaResponseDto media = uploadResultTask.getResult();
            String mediaId = media != null ? media.getId() : null;
            return assignmentDataSource.createAssignment(subjectId, title, description, finalDueDateMillis, finalMaxMarks, mediaId);
        }).addOnSuccessListener(v -> {
            uiState.setValue(uiState.getValue().toBuilder().saving(false).uploadProgress(null).build());
            created.setValue(true);
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                .saving(false).uploadProgress(null).errorMessage(e.getMessage()).build()));
    }
}
