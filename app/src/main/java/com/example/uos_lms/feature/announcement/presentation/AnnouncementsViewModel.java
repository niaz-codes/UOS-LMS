package com.example.uos_lms.feature.announcement.presentation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiContentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.AnnouncementScope;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.google.android.gms.tasks.Tasks;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AnnouncementsViewModel extends ViewModel {

    private final ApiContentDataSource contentDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final AuthApi authApi;

    private final MutableLiveData<AnnouncementsUiState> uiState = new MutableLiveData<>(AnnouncementsUiState.initial());

    @Inject
    public AnnouncementsViewModel(
            ApiContentDataSource contentDataSource,
            ApiUniversityDataSource universityDataSource,
            AuthApi authApi) {
        this.contentDataSource = contentDataSource;
        this.universityDataSource = universityDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<AnnouncementsUiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        RetrofitTasks.call(authApi.me()).addOnCompleteListener(meTask -> {
            User user = meTask.isSuccessful() && meTask.getResult() != null ? meTask.getResult().getUser().toDomain() : null;
            uiState.setValue(uiState.getValue().toBuilder().role(user != null ? user.getRole() : null).build());

            contentDataSource.listAnnouncements()
                    .addOnSuccessListener(announcements -> uiState.setValue(uiState.getValue().toBuilder().announcements(announcements).loading(false).build()))
                    .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));

            if (user == null) return;
            if (user.getRole() == UserRole.ADMIN) {
                universityDataSource.listDepartments().addOnSuccessListener(departments ->
                        uiState.setValue(uiState.getValue().toBuilder().departments(departments).build()));
            } else if (user.getRole() == UserRole.HOD && user.getDepartment() != null) {
                universityDataSource.listDepartments().addOnSuccessListener(departments -> {
                    Department mine = null;
                    for (Department department : departments) {
                        if (department.getId().equals(user.getDepartment())) {
                            mine = department;
                            break;
                        }
                    }
                    uiState.setValue(uiState.getValue().toBuilder()
                            .myDepartmentId(user.getDepartment())
                            .myDepartmentName(mine != null ? mine.getName() : null)
                            .build());
                });
            } else if (user.getRole() == UserRole.TEACHER) {
                universityDataSource.listSubjectsForTeacher(user.getUid()).addOnSuccessListener(subjects ->
                        uiState.setValue(uiState.getValue().toBuilder().mySubjects(subjects).build()));
            }
        });
    }

    public void postAll(String title, String body) {
        postScoped(title, body, AnnouncementScope.ALL.name(), null, null);
    }

    public void postToDepartment(String title, String body, Department department) {
        postScoped(title, body, AnnouncementScope.DEPARTMENT.name(), department.getId(), null);
    }

    public void postToMyDepartment(String title, String body) {
        AnnouncementsUiState state = uiState.getValue();
        if (state.getMyDepartmentId() == null) return;
        postScoped(title, body, AnnouncementScope.DEPARTMENT.name(), state.getMyDepartmentId(), null);
    }

    public void postToSubject(String title, String body, Subject subject) {
        postScoped(title, body, AnnouncementScope.SUBJECT.name(), null, subject.getId());
    }

    private void postScoped(String title, String body, String scope, String departmentId, String subjectId) {
        uiState.setValue(uiState.getValue().toBuilder().posting(true).errorMessage(null).build());
        contentDataSource.createAnnouncement(title, body, scope, departmentId, subjectId)
                .continueWithTask(created -> contentDataSource.listAnnouncements())
                .addOnSuccessListener(announcements -> uiState.setValue(uiState.getValue().toBuilder().posting(false).announcements(announcements).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().posting(false).errorMessage(e.getMessage()).build()));
    }

    public void deleteAnnouncement(String id) {
        contentDataSource.deleteAnnouncement(id)
                .continueWithTask(deleted -> contentDataSource.listAnnouncements())
                .addOnSuccessListener(announcements -> uiState.setValue(uiState.getValue().toBuilder().announcements(announcements).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().errorMessage(e.getMessage()).build()));
    }

    public void clearError() {
        uiState.setValue(uiState.getValue().toBuilder().errorMessage(null).build());
    }
}
