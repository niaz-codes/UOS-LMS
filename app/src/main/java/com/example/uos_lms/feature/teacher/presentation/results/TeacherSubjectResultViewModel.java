package com.example.uos_lms.feature.teacher.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherSubjectResultViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;
    private final String subjectId;

    private final MutableLiveData<TeacherSubjectResultUiState> uiState =
            new MutableLiveData<>(TeacherSubjectResultUiState.initial());

    @Inject
    public TeacherSubjectResultViewModel(SavedStateHandle savedStateHandle, ApiExamResultDataSource examResultDataSource) {
        this.examResultDataSource = examResultDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    public LiveData<TeacherSubjectResultUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        examResultDataSource.listResultsForSubject(subjectId)
                .addOnSuccessListener(results -> {
                    List<ExamResult> approved = new ArrayList<>();
                    for (ExamResult result : results) {
                        if (result.getStatus() == ResultStatus.APPROVED) approved.add(result);
                    }
                    approved.sort((a, b) -> a.getStudentName().compareToIgnoreCase(b.getStudentName()));
                    uiState.setValue(uiState.getValue().toBuilder().results(approved).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public void consumeError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
