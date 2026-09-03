package com.example.uos_lms.feature.teacher.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherResultsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<TeacherResultsUiState> uiState = new MutableLiveData<>(TeacherResultsUiState.initial());

    @Inject
    public TeacherResultsViewModel(ApiUniversityDataSource universityDataSource) {
        this.universityDataSource = universityDataSource;
        load();
    }

    public LiveData<TeacherResultsUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        universityDataSource.listSubjectsForTeacher("me")
                .addOnSuccessListener(subjects -> uiState.setValue(uiState.getValue().toBuilder().subjects(subjects).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public void consumeError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
