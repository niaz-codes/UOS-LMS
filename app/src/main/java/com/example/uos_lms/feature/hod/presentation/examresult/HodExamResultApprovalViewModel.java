package com.example.uos_lms.feature.hod.presentation.examresult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiExamResultDataSource;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodExamResultApprovalViewModel extends ViewModel {

    private final ApiExamResultDataSource examResultDataSource;

    private final MediatorLiveData<HodExamResultUiState> uiState = new MediatorLiveData<>(HodExamResultUiState.initial());

    @Inject
    public HodExamResultApprovalViewModel(ApiExamResultDataSource examResultDataSource) {
        this.examResultDataSource = examResultDataSource;
        load();
    }

    public LiveData<HodExamResultUiState> getUiState() {
        return uiState;
    }

    /** Both tabs are server-scoped to the reviewer's own department already (see
     * examResultController.list) - no separate "look up my department" round trip needed
     * like the Firestore version required. */
    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        Tasks.whenAllSuccess(
                examResultDataSource.listPendingApprovals(),
                examResultDataSource.listForCurrentRole()
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<ExamResult> pending = new ArrayList<>((List<ExamResult>) results.get(0));
            pending.sort((a, b) -> a.getStudentName().compareToIgnoreCase(b.getStudentName()));
            //noinspection unchecked
            List<ExamResult> all = (List<ExamResult>) results.get(1);
            uiState.setValue(uiState.getValue().toBuilder().pending(pending).all(all).loading(false).build());
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                .loading(false).errorMessage(e.getMessage()).build()));
    }

    public void selectTab(ResultReviewTab tab) {
        uiState.setValue(uiState.getValue().toBuilder().tab(tab).build());
    }

    public void approve(ExamResult result) {
        uiState.setValue(uiState.getValue().toBuilder().processingResultId(result.getId()).errorMessage(null).build());
        examResultDataSource.approve(result.getId())
                .addOnSuccessListener(approved -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingResultId(null)
                            .actionMessage(String.format(Locale.getDefault(), "Approved %s's result (%s, %.2f GPA).",
                                    approved.getStudentName(), approved.getGrade(), approved.getGpaPoint()))
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingResultId(null).errorMessage(e.getMessage()).build()));
    }

    public void reject(ExamResult result, String reason) {
        uiState.setValue(uiState.getValue().toBuilder().processingResultId(result.getId()).errorMessage(null).build());
        examResultDataSource.reject(result.getId(), reason)
                .addOnSuccessListener(rejected -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingResultId(null)
                            .actionMessage("Rejected " + rejected.getStudentName() + "'s result.")
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingResultId(null).errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
