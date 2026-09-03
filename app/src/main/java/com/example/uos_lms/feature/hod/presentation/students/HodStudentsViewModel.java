package com.example.uos_lms.feature.hod.presentation.students;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.UserRole;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodStudentsViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthApi authApi;

    private final MutableLiveData<HodStudentsUiState> uiState = new MutableLiveData<>(HodStudentsUiState.initial());

    @Inject
    public HodStudentsViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthApi authApi) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<HodStudentsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the roster + semesters from the backend without blanking the currently-shown
     * list, preserving the user's current search query / semester filter - no-ops while a
     * refresh is already in flight. */
    public void refresh() {
        HodStudentsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    uiState.setValue(uiState.getValue().toBuilder().departmentId(departmentId).build());
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }

                    userDataSource.listUsersInDepartmentByRole(departmentId, UserRole.STUDENT)
                            .addOnSuccessListener(students ->
                                    uiState.setValue(uiState.getValue().toBuilder().allStudents(students).loading(false).refreshing(false).build()))
                            .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));

                    universityDataSource.listSemesters(departmentId)
                            .addOnSuccessListener(semesters -> {
                                List<Semester> sorted = new ArrayList<>(semesters);
                                sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
                                uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).build());
                            });
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public void onSearchQueryChange(String query) {
        uiState.setValue(uiState.getValue().toBuilder().searchQuery(query).build());
    }

    public void onSemesterSelected(Semester semester) {
        uiState.setValue(uiState.getValue().toBuilder().selectedSemester(semester).build());
    }
}
