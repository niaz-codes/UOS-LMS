package com.example.uos_lms.feature.hod.presentation.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Department;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodResultsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;

    private final MutableLiveData<HodResultsUiState> uiState = new MutableLiveData<>(HodResultsUiState.initial());

    @Inject
    public HodResultsViewModel(ApiUniversityDataSource universityDataSource, AuthApi authApi) {
        this.universityDataSource = universityDataSource;
        load(authApi);
    }

    public LiveData<HodResultsUiState> getUiState() {
        return uiState;
    }

    private void load(AuthApi authApi) {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null || user.getDepartment() == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    universityDataSource.listDepartments().addOnSuccessListener(departments -> {
                        String departmentName = "";
                        for (Department department : departments) {
                            if (department.getId().equals(user.getDepartment())) {
                                departmentName = department.getName();
                                break;
                            }
                        }
                        uiState.setValue(uiState.getValue().toBuilder()
                                .departmentId(user.getDepartment())
                                .departmentName(departmentName)
                                .loading(false)
                                .build());
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }
}
