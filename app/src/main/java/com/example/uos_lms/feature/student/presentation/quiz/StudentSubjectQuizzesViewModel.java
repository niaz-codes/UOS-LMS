package com.example.uos_lms.feature.student.presentation.quiz;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentSubjectQuizzesViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final String subjectId;

    private final MutableLiveData<StudentSubjectQuizzesUiState> uiState =
            new MutableLiveData<>(StudentSubjectQuizzesUiState.initial());

    private List<Quiz> latestQuizzes;
    private final Map<String, QuizAttempt> attemptByQuiz = new HashMap<>();

    @Inject
    public StudentSubjectQuizzesViewModel(
            SavedStateHandle savedStateHandle,
            ApiQuizDataSource quizDataSource) {
        this.quizDataSource = quizDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        load();
    }

    public LiveData<StudentSubjectQuizzesUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's quizzes/exams (and this student's attempts) from the backend
     * without blanking the currently-shown list - no-ops while a refresh is already in flight.
     * Also the entry point for refreshing automatically when returning from Take Quiz (see
     * Fragment.onResume). */
    public void refresh() {
        StudentSubjectQuizzesUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        quizDataSource.quizzesForSubject(subjectId)
                .addOnSuccessListener(quizzes -> {
                    latestQuizzes = quizzes;
                    List<Task<QuizAttempt>> tasks = new ArrayList<>();
                    for (Quiz quiz : quizzes) {
                        tasks.add(quizDataSource.myAttemptForQuiz(quiz.getId())
                                .addOnSuccessListener(attempt -> attemptByQuiz.put(quiz.getId(), attempt)));
                    }
                    Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute() {
        if (latestQuizzes == null) return;
        List<Quiz> sorted = new ArrayList<>(latestQuizzes);
        sorted.sort(Comparator.comparingLong(Quiz::getDueDateMillis).reversed());

        List<QuizListItem> items = new ArrayList<>();
        for (Quiz quiz : sorted) {
            items.add(QuizListItem.builder().quiz(quiz).attempt(attemptByQuiz.get(quiz.getId())).build());
        }

        uiState.setValue(uiState.getValue().toBuilder().items(items).loading(false).refreshing(false).build());
    }
}
