package com.example.uos_lms.feature.admin.university.presentation.department;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Session;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DepartmentDetailViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final String departmentId;

    private final MutableLiveData<DepartmentDetailUiState> uiState =
            new MutableLiveData<>(DepartmentDetailUiState.initial());

    @Inject
    public DepartmentDetailViewModel(
            SavedStateHandle savedStateHandle,
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.departmentId = savedStateHandle.get("departmentId");
        load();
    }

    public LiveData<DepartmentDetailUiState> getUiState() {
        return uiState;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    /** Re-fetches the department + sessions from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        DepartmentDetailUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.listDepartments().addOnSuccessListener(departments -> {
            Department department = null;
            for (Department candidate : departments) {
                if (candidate.getId().equals(departmentId)) {
                    department = candidate;
                    break;
                }
            }
            uiState.setValue(uiState.getValue().toBuilder().department(department).build());
        });
        loadSessions();
    }

    private void loadSessions() {
        universityDataSource.listSessions(departmentId)
                .addOnSuccessListener(sessions -> {
                    List<Session> sorted = new ArrayList<>(sessions);
                    sorted.sort((a, b) -> b.getLabel().compareTo(a.getLabel()));
                    uiState.setValue(uiState.getValue().toBuilder().sessions(sorted).loading(false).refreshing(false).build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public Task<Department> updateDepartment(String name, String code, String description) {
        return universityDataSource.updateDepartment(departmentId, name, code, description)
                .addOnSuccessListener(department -> uiState.setValue(uiState.getValue().toBuilder().department(department).build()));
    }

    public void deleteDepartment(Runnable onDeleted) {
        universityDataSource.deleteDepartment(departmentId)
                .addOnSuccessListener(v -> onDeleted.run())
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public Task<Session> addSession(String label) {
        return universityDataSource.createSession(departmentId, label).addOnSuccessListener(v -> loadSessions());
    }

    public Task<Session> editSessionLabel(String id, String label) {
        return universityDataSource.updateSession(id, label, null).addOnSuccessListener(v -> loadSessions());
    }

    public void setSessionActive(Session session, boolean isActive) {
        DepartmentDetailUiState state = uiState.getValue();
        if (session.isActive() == isActive) return;

        List<Session> optimistic = new ArrayList<>();
        for (Session existing : state.getSessions()) {
            optimistic.add(existing.getId().equals(session.getId()) ? existing.toBuilder().isActive(isActive).build() : existing);
        }
        uiState.setValue(state.toBuilder().sessions(optimistic).errorMessage(null).build());

        universityDataSource.updateSession(session.getId(), null, isActive)
                .addOnFailureListener(e -> {
                    DepartmentDetailUiState current = uiState.getValue();
                    List<Session> reverted = new ArrayList<>();
                    for (Session existing : current.getSessions()) {
                        reverted.add(existing.getId().equals(session.getId()) ? session : existing);
                    }
                    uiState.setValue(current.toBuilder().sessions(reverted).errorMessage(e.getMessage()).build());
                });
    }

    public void requestDeleteSession(Session session) {
        uiState.setValue(uiState.getValue().toBuilder().checkingSessionDelete(true).build());
        userDataSource.countStudentsBySessionId(session.getId())
                .addOnSuccessListener(count -> uiState.setValue(uiState.getValue().toBuilder()
                        .checkingSessionDelete(false)
                        .sessionDeleteCheck(SessionDeleteCheck.builder().session(session).linkedStudentCount(count).build())
                        .build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .checkingSessionDelete(false).errorMessage(e.getMessage()).build()));
    }

    public void cancelSessionDelete() {
        uiState.setValue(uiState.getValue().toBuilder().sessionDeleteCheck(null).build());
    }

    public void deleteSession(String id) {
        universityDataSource.deleteSession(id)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().sessionDeleteCheck(null).build());
                    loadSessions();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().sessionDeleteCheck(null).errorMessage(e.getMessage()).build()));
    }

    public void reassignAndDeleteSession(String fromSessionId, String toSessionId) {
        universityDataSource.reassignSessionStudents(fromSessionId, toSessionId)
                .continueWithTask(task -> universityDataSource.deleteSession(fromSessionId))
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().sessionDeleteCheck(null).build());
                    loadSessions();
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().sessionDeleteCheck(null).errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
