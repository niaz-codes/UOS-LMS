package com.example.uos_lms.feature.teacher.presentation.quiz;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.domain.model.Quiz;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherSubjectQuizzesViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final String subjectId;

    private final MutableLiveData<TeacherSubjectQuizzesUiState> uiState =
            new MutableLiveData<>(TeacherSubjectQuizzesUiState.initial());

    @Inject
    public TeacherSubjectQuizzesViewModel(SavedStateHandle savedStateHandle, ApiQuizDataSource quizDataSource) {
        this.quizDataSource = quizDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    public LiveData<TeacherSubjectQuizzesUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's quizzes/exams from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        TeacherSubjectQuizzesUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        quizDataSource.quizzesForSubject(subjectId)
                .addOnSuccessListener(quizzes -> {
                    List<Quiz> sorted = new ArrayList<>(quizzes);
                    sorted.sort((a, b) -> Long.compare(b.getDueDateMillis(), a.getDueDateMillis()));
                    uiState.setValue(uiState.getValue().toBuilder().quizzes(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }
}
