package com.example.uos_lms.feature.admin.presentation.quizzes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.Subject;
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
public class AdminQuizMonitorViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<AdminQuizMonitorUiState> uiState =
            new MutableLiveData<>(AdminQuizMonitorUiState.initial());

    @Inject
    public AdminQuizMonitorViewModel(ApiQuizDataSource quizDataSource, ApiUniversityDataSource universityDataSource) {
        this.quizDataSource = quizDataSource;
        this.universityDataSource = universityDataSource;
        load();
    }

    private void load() {
        universityDataSource.listDepartments().onSuccessTask(departments -> {
            List<Task<List<Subject>>> subjectTasks = new ArrayList<>();
            for (Department department : departments) {
                subjectTasks.add(universityDataSource.listSubjectsForDepartment(department.getId()));
            }
            return Tasks.whenAllSuccess(subjectTasks);
        }).addOnSuccessListener(results -> {
            List<Subject> allSubjects = new ArrayList<>();
            for (Object result : results) {
                //noinspection unchecked
                allSubjects.addAll((List<Subject>) result);
            }
            Map<String, Subject> byId = new HashMap<>();
            for (Subject subject : allSubjects) byId.put(subject.getId(), subject);

            quizDataSource.allQuizzes().addOnSuccessListener(quizzes -> {
                List<Quiz> sorted = new ArrayList<>(quizzes);
                sorted.sort(Comparator.comparingLong(Quiz::getDueDateMillis).reversed());
                uiState.setValue(uiState.getValue().toBuilder().quizzes(sorted).subjectsById(byId).loading(false).build());
            }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
    }

    public LiveData<AdminQuizMonitorUiState> getUiState() {
        return uiState;
    }

    /** Permanently deletes the quiz (and every student attempt against it - see
     * quizController.remove) and refreshes the list. */
    public void deleteQuiz(String quizId) {
        quizDataSource.deleteQuiz(quizId)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
