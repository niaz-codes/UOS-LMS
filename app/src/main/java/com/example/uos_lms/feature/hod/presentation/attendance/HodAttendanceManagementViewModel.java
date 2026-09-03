package com.example.uos_lms.feature.hod.presentation.attendance;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodAttendanceManagementViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private List<Subject> allSubjects = new ArrayList<>();

    private final MutableLiveData<HodAttendanceManagementUiState> uiState =
            new MutableLiveData<>(HodAttendanceManagementUiState.initial());

    @Inject
    public HodAttendanceManagementViewModel(ApiUniversityDataSource universityDataSource, AuthApi authApi) {
        this.universityDataSource = universityDataSource;
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    uiState.setValue(uiState.getValue().toBuilder().departmentId(departmentId).build());
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    Tasks.whenAllSuccess(
                            universityDataSource.listSemesters(departmentId),
                            universityDataSource.listSubjectsForDepartment(departmentId)
                    ).addOnSuccessListener(results -> {
                        //noinspection unchecked
                        List<Semester> semesters = (List<Semester>) results.get(0);
                        //noinspection unchecked
                        List<Subject> subjects = (List<Subject>) results.get(1);
                        List<Semester> sorted = new ArrayList<>(semesters);
                        sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
                        allSubjects = subjects;
                        uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).loading(false).build());
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<HodAttendanceManagementUiState> getUiState() {
        return uiState;
    }

    public void onSemesterSelected(@Nullable Semester semester) {
        List<Subject> inSemester = new ArrayList<>();
        if (semester != null) {
            for (Subject subject : allSubjects) {
                if (semester.getId().equals(subject.getSemesterId())) inSemester.add(subject);
            }
        }
        uiState.setValue(uiState.getValue().toBuilder()
                .selectedSemester(semester)
                .subjectsInSemester(inSemester)
                .selectedSubject(null)
                .build());
    }

    public void onSubjectSelected(@Nullable Subject subject) {
        uiState.setValue(uiState.getValue().toBuilder().selectedSubject(subject).build());
    }

    public void onTeacherSemesterSelected(@Nullable Semester semester) {
        uiState.setValue(uiState.getValue().toBuilder().selectedTeacherSemester(semester).build());
    }
}
