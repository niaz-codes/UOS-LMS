package com.example.uos_lms.feature.teacher.presentation.quiz;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.domain.model.QuizAttempt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class QuizAttemptsViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final String quizId;

    private final MutableLiveData<QuizAttemptsUiState> uiState =
            new MutableLiveData<>(QuizAttemptsUiState.initial());

    @Inject
    public QuizAttemptsViewModel(
            SavedStateHandle savedStateHandle,
            ApiQuizDataSource quizDataSource) {
        this.quizDataSource = quizDataSource;
        this.quizId = savedStateHandle.get("quizId");
        load();
    }

    private void load() {
        quizDataSource.getQuiz(quizId).addOnSuccessListener(quiz -> {
            if (quiz != null) uiState.setValue(uiState.getValue().toBuilder().quiz(quiz).build());
        });

        quizDataSource.attemptsForQuiz(quizId)
                .addOnSuccessListener(attempts -> {
                    List<QuizAttempt> sorted = new ArrayList<>(attempts);
                    sorted.sort(Comparator.comparing(QuizAttempt::getStudentName));
                    uiState.setValue(uiState.getValue().toBuilder().attempts(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<QuizAttemptsUiState> getUiState() {
        return uiState;
    }

    public void gradeAttempt(String attemptId, int manualScore, String feedback) {
        quizDataSource.gradeAttempt(attemptId, manualScore, feedback)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
