package com.example.uos_lms.feature.hod.presentation.teachers;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodTeachersViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiUserDataSource userDataSource;
    private final AuthApi authApi;

    private final MutableLiveData<HodTeachersUiState> uiState = new MutableLiveData<>(HodTeachersUiState.initial());

    @Inject
    public HodTeachersViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiUserDataSource userDataSource,
            AuthApi authApi) {
        this.universityDataSource = universityDataSource;
        this.userDataSource = userDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<HodTeachersUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches from the backend without blanking the currently-shown list - the pull-to-
     * refresh / toolbar-refresh entry point for this screen. No-ops while a refresh is already
     * in flight so a fast double-tap can't fire a second request. */
    public void refresh() {
        HodTeachersUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null || user.getDepartment() == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    String departmentId = user.getDepartment();
                    Tasks.whenAllSuccess(
                            userDataSource.listApprovedTeachersInDepartment(departmentId),
                            universityDataSource.listSubjectsForDepartment(departmentId)
                    ).addOnSuccessListener(results -> {
                        //noinspection unchecked
                        List<User> teachers = (List<User>) results.get(0);
                        //noinspection unchecked
                        List<Subject> subjects = (List<Subject>) results.get(1);
                        recombine(teachers, subjects);
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void recombine(List<User> teachers, List<Subject> subjects) {
        List<TeacherWithLoad> result = new ArrayList<>();
        for (User teacher : teachers) {
            int count = 0;
            for (Subject subject : subjects) {
                if (teacher.getUid().equals(subject.getTeacherUid())) count++;
            }
            result.add(TeacherWithLoad.builder().teacher(teacher).subjectCount(count).build());
        }
        result.sort((a, b) -> Integer.compare(b.getSubjectCount(), a.getSubjectCount()));
        uiState.setValue(uiState.getValue().toBuilder().teachers(result).loading(false).refreshing(false).build());
    }
}
