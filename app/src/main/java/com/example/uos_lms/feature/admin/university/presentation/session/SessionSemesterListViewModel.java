package com.example.uos_lms.feature.admin.university.presentation.session;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Middle of the Admin Department Management hierarchy: Session -> Semester, scoped to the
 * (departmentId, sessionId) selected on DepartmentDetailFragment. Semesters themselves aren't
 * session-scoped in the data model (a department's semester list is the same regardless of
 * session) - this screen just presents that same list in the navigational context of the
 * chosen session, matching where "Add Subject" leads next.
 */
@HiltViewModel
public class SessionSemesterListViewModel extends ViewModel {

    private final ApiUniversityDataSource dataSource;
    private final String departmentId;
    private final String sessionId;

    private final MutableLiveData<SessionSemesterListUiState> uiState =
            new MutableLiveData<>(SessionSemesterListUiState.initial());

    @Inject
    public SessionSemesterListViewModel(SavedStateHandle savedStateHandle, ApiUniversityDataSource dataSource) {
        this.dataSource = dataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        this.sessionId = savedStateHandle.get("sessionId");

        dataSource.listDepartments().addOnSuccessListener(departments -> {
            for (Department department : departments) {
                if (department.getId().equals(departmentId)) {
                    uiState.setValue(uiState.getValue().toBuilder().departmentName(department.getName()).build());
                    break;
                }
            }
        });
        dataSource.listSessions(departmentId).addOnSuccessListener(sessions -> {
            for (Session session : sessions) {
                if (session.getId().equals(sessionId)) {
                    uiState.setValue(uiState.getValue().toBuilder().sessionLabel(session.getLabel()).build());
                    break;
                }
            }
        });
        loadSemesters();
    }

    public LiveData<SessionSemesterListUiState> getUiState() {
        return uiState;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    /** Re-fetches this department's semesters from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        SessionSemesterListUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        loadSemesters();
    }

    private void loadSemesters() {
        dataSource.listSemesters(departmentId)
                .addOnSuccessListener(semesters -> {
                    List<Semester> sorted = new ArrayList<>(semesters);
                    sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
                    uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public Task<Semester> addSemester(int number) {
        return dataSource.createSemester(departmentId, number).addOnSuccessListener(v -> loadSemesters());
    }

    public void deleteSemester(String id) {
        dataSource.deleteSemester(id)
                .addOnSuccessListener(v -> loadSemesters())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
