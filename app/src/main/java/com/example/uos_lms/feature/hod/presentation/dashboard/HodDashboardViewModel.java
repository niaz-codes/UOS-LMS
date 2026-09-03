package com.example.uos_lms.feature.hod.presentation.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodDashboardViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthApi authApi;
    private final SessionManager sessionManager;

    private final MutableLiveData<HodDashboardUiState> uiState =
            new MutableLiveData<>(HodDashboardUiState.initial());

    @Inject
    public HodDashboardViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthApi authApi,
            SessionManager sessionManager) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authApi = authApi;
        this.sessionManager = sessionManager;
        loadHod();
    }

    public LiveData<HodDashboardUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches everything on this dashboard from the backend without blanking the currently-
     * shown stats - no-ops while a refresh is already in flight. */
    public void refresh() {
        HodDashboardUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        loadHod();
    }

    private void loadHod() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage("Session expired. Please log in again.").build());
                        return;
                    }
                    uiState.setValue(uiState.getValue().toBuilder().fullName(user.getFullName()).departmentId(user.getDepartment()).build());
                    if (user.getDepartment() == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    loadDepartmentData(user.getDepartment());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void loadDepartmentData(String departmentId) {
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (department.getId().equals(departmentId)) {
                    uiState.setValue(uiState.getValue().toBuilder().departmentName(department.getName()).build());
                    break;
                }
            }
        }).addOnFailureListener(e -> { });

        universityDataSource.listSemesters(departmentId).addOnSuccessListener(semesters -> {
            List<Semester> sorted = new ArrayList<>(semesters);
            sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
            uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).loading(false).refreshing(false).build());
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));

        userDataSource.countInDepartmentByRole(departmentId, UserRole.TEACHER).addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().teacherCount(count).build())).addOnFailureListener(e -> { });

        userDataSource.countInDepartmentByRole(departmentId, UserRole.STUDENT).addOnSuccessListener(count ->
                uiState.setValue(uiState.getValue().toBuilder().studentCount(count).build())).addOnFailureListener(e -> { });
    }

    public void logout() {
        sessionManager.clear();
    }
}
