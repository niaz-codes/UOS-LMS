package com.example.uos_lms.feature.teacher.presentation.assignment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.domain.model.Assignment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherSubjectAssignmentsViewModel extends ViewModel {

    private final ApiAssignmentDataSource assignmentDataSource;
    private final String subjectId;

    private final MutableLiveData<TeacherSubjectAssignmentsUiState> uiState =
            new MutableLiveData<>(TeacherSubjectAssignmentsUiState.initial());

    @Inject
    public TeacherSubjectAssignmentsViewModel(SavedStateHandle savedStateHandle, ApiAssignmentDataSource assignmentDataSource) {
        this.assignmentDataSource = assignmentDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    public LiveData<TeacherSubjectAssignmentsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's assignments from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. Also the entry point for
     * refreshing automatically when returning from Create Assignment (see Fragment.onResume). */
    public void refresh() {
        TeacherSubjectAssignmentsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        assignmentDataSource.assignmentsForSubject(subjectId)
                .addOnSuccessListener(assignments -> {
                    List<Assignment> sorted = new ArrayList<>(assignments);
                    sorted.sort((a, b) -> Long.compare(b.getDueDateMillis(), a.getDueDateMillis()));
                    uiState.setValue(uiState.getValue().toBuilder().assignments(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }
}
