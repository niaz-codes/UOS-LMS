package com.example.uos_lms.feature.student.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentResultsViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;

    private final MutableLiveData<StudentResultsUiState> uiState = new MutableLiveData<>(StudentResultsUiState.initial());

    @Inject
    public StudentResultsViewModel(ApiExamResultDataSource examResultDataSource) {
        this.examResultDataSource = examResultDataSource;
        load();
    }

    public LiveData<StudentResultsUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        fetch();
    }

    /** Re-fetches results/CGPA from the backend without blanking the currently-shown card -
     * no-ops while a refresh is already in flight. */
    public void refresh() {
        StudentResultsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        fetch();
    }

    private void fetch() {
        examResultDataSource.listSemesterResults(null, null, null, null)
                .addOnSuccessListener(this::recompute)
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute(List<StudentSemesterResultSummary> results) {
        String sessionLabel = "";
        int totalCreditHours = 0;
        double cgpa = 0.0;
        int highestApprovedSemesterNumber = -1;

        for (StudentSemesterResultSummary result : results) {
            if (sessionLabel.isEmpty() && !result.getSessionLabel().isEmpty()) {
                sessionLabel = result.getSessionLabel();
            }
            if (result.getResultStatus() != ResultStatus.APPROVED) continue;
            totalCreditHours += result.getSubjectResults().stream().mapToInt(s -> s.getCreditHours()).sum();
            if (result.getSemesterNumber() > highestApprovedSemesterNumber) {
                highestApprovedSemesterNumber = result.getSemesterNumber();
                cgpa = result.getCumulativeCgpa();
            }
        }

        uiState.setValue(uiState.getValue().toBuilder()
                .cgpa(cgpa)
                .totalCreditHours(totalCreditHours)
                .sessionLabel(sessionLabel)
                .semesterCount(results.size())
                .hasResults(!results.isEmpty())
                .results(results)
                .loading(false)
                .refreshing(false)
                .build());
    }

    public void consumeError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
