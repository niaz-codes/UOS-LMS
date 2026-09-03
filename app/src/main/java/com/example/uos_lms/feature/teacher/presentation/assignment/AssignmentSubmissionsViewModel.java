package com.example.uos_lms.feature.teacher.presentation.assignment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AssignmentSubmissionsViewModel extends ViewModel {

    private final ApiAssignmentDataSource assignmentDataSource;
    private final String assignmentId;

    private final MutableLiveData<AssignmentSubmissionsUiState> uiState =
            new MutableLiveData<>(AssignmentSubmissionsUiState.initial());

    @Inject
    public AssignmentSubmissionsViewModel(
            SavedStateHandle savedStateHandle,
            ApiAssignmentDataSource assignmentDataSource) {
        this.assignmentDataSource = assignmentDataSource;
        this.assignmentId = savedStateHandle.get("assignmentId");
        load();
    }

    private void load() {
        assignmentDataSource.submissionsForAssignment(assignmentId)
                .addOnSuccessListener(submissions -> {
                    List<AssignmentSubmission> sorted = new ArrayList<>(submissions);
                    sorted.sort(Comparator.comparing(AssignmentSubmission::getStudentName));
                    uiState.setValue(uiState.getValue().toBuilder().submissions(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<AssignmentSubmissionsUiState> getUiState() {
        return uiState;
    }

    public void grade(AssignmentSubmission submission, int marksObtained, String feedback) {
        assignmentDataSource.gradeSubmission(submission.getId(), marksObtained, feedback)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
