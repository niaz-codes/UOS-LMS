package com.example.uos_lms.feature.hod.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import lombok.Builder;
import lombok.Getter;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodSemesterResultViewModel extends ViewModel {

    @Getter
    @Builder(toBuilder = true)
    public static class UiState {
        @Builder.Default
        private final List<StudentSemesterResultSummary> results = Collections.emptyList();
        @Builder.Default
        private final boolean loading = true;
        private final String errorMessage;

        static UiState initial() {
            return UiState.builder().build();
        }
    }

    private final ApiExamResultDataSource examResultDataSource;
    private final String departmentId;
    private final String sessionId;
    private final String semesterId;

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.initial());

    @Inject
    public HodSemesterResultViewModel(SavedStateHandle savedStateHandle, ApiExamResultDataSource examResultDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        this.sessionId = savedStateHandle.get("sessionId");
        this.semesterId = savedStateHandle.get("semesterId");
        load();
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        examResultDataSource.listSemesterResults(departmentId, sessionId, semesterId, null)
                .addOnSuccessListener(results -> {
                    List<StudentSemesterResultSummary> sorted = new ArrayList<>(results);
                    sorted.sort((a, b) -> a.getStudentName().compareToIgnoreCase(b.getStudentName()));
                    uiState.setValue(uiState.getValue().toBuilder().results(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
