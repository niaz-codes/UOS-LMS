package com.example.uos_lms.feature.admin.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Session;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import lombok.Builder;
import lombok.Getter;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminResultsSessionListViewModel extends ViewModel {

    @Getter
    @Builder(toBuilder = true)
    public static class UiState {
        @Builder.Default
        private final List<Session> sessions = Collections.emptyList();
        @Builder.Default
        private final boolean loading = true;
        private final String errorMessage;

        static UiState initial() {
            return UiState.builder().build();
        }
    }

    private final ApiUniversityDataSource universityDataSource;
    private final String departmentId;

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.initial());

    @Inject
    public AdminResultsSessionListViewModel(SavedStateHandle savedStateHandle, ApiUniversityDataSource universityDataSource) {
        this.universityDataSource = universityDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        load();
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        universityDataSource.listSessions(departmentId)
                .addOnSuccessListener(sessions -> uiState.setValue(uiState.getValue().toBuilder().sessions(sessions).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
