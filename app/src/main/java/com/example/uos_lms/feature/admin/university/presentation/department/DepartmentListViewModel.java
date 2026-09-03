package com.example.uos_lms.feature.admin.university.presentation.department;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DepartmentListViewModel extends ViewModel {

    private final ApiUniversityDataSource dataSource;

    private final MutableLiveData<DepartmentListUiState> uiState =
            new MutableLiveData<>(DepartmentListUiState.initial());

    @Inject
    public DepartmentListViewModel(ApiUniversityDataSource dataSource) {
        this.dataSource = dataSource;
        load();
    }

    private void load() {
        dataSource.listDepartments()
                .addOnSuccessListener(departments -> uiState.setValue(uiState.getValue().toBuilder().departments(departments).loading(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).listErrorMessage(e.getMessage()).build()));
    }

    public LiveData<DepartmentListUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches departments from the backend without blanking the currently-shown list -
     * no-ops while a refresh is already in flight. */
    public void refresh() {
        DepartmentListUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).listErrorMessage(null).build());
        load();
    }

    public Task<Department> createDepartment(String name, String code, String description) {
        return dataSource.createDepartment(name, code, description).addOnSuccessListener(v -> load());
    }

    public Task<Department> updateDepartment(String id, String name, String code, String description) {
        return dataSource.updateDepartment(id, name, code, description).addOnSuccessListener(v -> load());
    }

    public void deleteDepartment(String id) {
        dataSource.deleteDepartment(id)
                .addOnSuccessListener(v -> load())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().listErrorMessage(e.getMessage()).build()));
    }

    public void clearListError() {
        uiState.setValue(uiState.getValue().toBuilder().listErrorMessage(null).build());
    }
}
