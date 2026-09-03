package com.example.uos_lms.feature.hod.presentation.quizzes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodQuizMonitorViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<HodQuizMonitorUiState> uiState = new MutableLiveData<>(HodQuizMonitorUiState.initial());

    @Inject
    public HodQuizMonitorViewModel(
            ApiQuizDataSource quizDataSource,
            ApiUniversityDataSource universityDataSource,
            AuthApi authApi) {
        this.quizDataSource = quizDataSource;
        this.universityDataSource = universityDataSource;
        load(authApi);
    }

    public LiveData<HodQuizMonitorUiState> getUiState() {
        return uiState;
    }

    private void load(AuthApi authApi) {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    universityDataSource.listSubjectsForDepartment(departmentId).addOnSuccessListener(subjects -> {
                        Map<String, Subject> subjectsById = new HashMap<>();
                        for (Subject subject : subjects) subjectsById.put(subject.getId(), subject);

                        quizDataSource.quizzesForDepartment(departmentId).addOnSuccessListener(quizzes -> {
                            List<Quiz> sorted = new ArrayList<>(quizzes);
                            sorted.sort((a, b) -> Long.compare(b.getDueDateMillis(), a.getDueDateMillis()));
                            uiState.setValue(uiState.getValue().toBuilder()
                                    .quizzes(sorted)
                                    .subjectsById(subjectsById)
                                    .loading(false)
                                    .build());
                        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
