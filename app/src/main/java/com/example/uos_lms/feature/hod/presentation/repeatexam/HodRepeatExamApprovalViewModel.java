package com.example.uos_lms.feature.hod.presentation.repeatexam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiRepeatExamDataSource;
import com.example.uos_lms.core.domain.model.RepeatExam;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Department-wide (server auto-scoped, see repeatExamController.list) queue of repeat
 * exams awaiting HOD review - mirrors HodExamResultApprovalViewModel's approve/reject
 * pattern, just without the "all" tab since there's no equivalent full-history view yet. */
@HiltViewModel
public class HodRepeatExamApprovalViewModel extends ViewModel {

    private final ApiRepeatExamDataSource repeatExamDataSource;

    private final MutableLiveData<HodRepeatExamUiState> uiState = new MutableLiveData<>(HodRepeatExamUiState.initial());

    @Inject
    public HodRepeatExamApprovalViewModel(ApiRepeatExamDataSource repeatExamDataSource) {
        this.repeatExamDataSource = repeatExamDataSource;
        load();
    }

    public LiveData<HodRepeatExamUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        repeatExamDataSource.listPendingReview()
                .addOnSuccessListener(list -> {
                    List<RepeatExam> sorted = new ArrayList<>(list);
                    sorted.sort((a, b) -> a.getStudentName().compareToIgnoreCase(b.getStudentName()));
                    uiState.setValue(uiState.getValue().toBuilder().pending(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .loading(false).errorMessage(e.getMessage()).build()));
    }

    public void approve(RepeatExam repeatExam) {
        uiState.setValue(uiState.getValue().toBuilder().processingRepeatExamId(repeatExam.getId()).errorMessage(null).build());
        repeatExamDataSource.approve(repeatExam.getId())
                .addOnSuccessListener(approved -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingRepeatExamId(null)
                            .actionMessage("Approved " + approved.getStudentName() + "'s repeat exam.")
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingRepeatExamId(null).errorMessage(e.getMessage()).build()));
    }

    public void reject(RepeatExam repeatExam, String reason) {
        uiState.setValue(uiState.getValue().toBuilder().processingRepeatExamId(repeatExam.getId()).errorMessage(null).build());
        repeatExamDataSource.reject(repeatExam.getId(), reason)
                .addOnSuccessListener(rejected -> {
                    uiState.setValue(uiState.getValue().toBuilder()
                            .processingRepeatExamId(null)
                            .actionMessage("Rejected " + rejected.getStudentName() + "'s repeat exam.")
                            .build());
                    load();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .processingRepeatExamId(null).errorMessage(e.getMessage()).build()));
    }

    public void consumeMessages() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).actionMessage(null).build());
    }
}
