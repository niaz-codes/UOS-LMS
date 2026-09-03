package com.example.uos_lms.feature.admin.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Department;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import lombok.Builder;
import lombok.Getter;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminResultsDepartmentListViewModel extends ViewModel {

    @Getter
    @Builder(toBuilder = true)
    public static class UiState {
        @Builder.Default
        private final List<Department> departments = Collections.emptyList();
        @Builder.Default
        private final boolean loading = true;
        private final String errorMessage;

        static UiState initial() {
            return UiState.builder().build();
        }
    }

    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.initial());

    @Inject
    public AdminResultsDepartmentListViewModel(ApiUniversityDataSource universityDataSource) {
        this.universityDataSource = universityDataSource;
        load();
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        universityDataSource.listDepartments()
                .addOnSuccessListener(departments -> {
                    List<Department> sorted = new ArrayList<>(departments);
                    sorted.sort(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER));
                    uiState.setValue(uiState.getValue().toBuilder().departments(sorted).loading(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
