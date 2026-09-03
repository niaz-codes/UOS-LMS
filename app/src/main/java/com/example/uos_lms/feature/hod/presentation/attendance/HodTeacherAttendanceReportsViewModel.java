package com.example.uos_lms.feature.hod.presentation.attendance;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiTeacherAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUserDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.example.uos_lms.core.domain.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodTeacherAttendanceReportsViewModel extends ViewModel {

    private final ApiTeacherAttendanceDataSource teacherAttendanceDataSource;

    private final MutableLiveData<HodTeacherAttendanceReportsUiState> uiState =
            new MutableLiveData<>(HodTeacherAttendanceReportsUiState.initial());

    @Inject
    public HodTeacherAttendanceReportsViewModel(
            ApiTeacherAttendanceDataSource teacherAttendanceDataSource,
            ApiUserDataSource userDataSource,
            ApiUniversityDataSource universityDataSource,
            AuthApi authApi) {
        this.teacherAttendanceDataSource = teacherAttendanceDataSource;
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    uiState.setValue(uiState.getValue().toBuilder().departmentId(departmentId).build());
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    userDataSource.listApprovedTeachersInDepartment(departmentId)
                            .addOnSuccessListener(teachers ->
                                    uiState.setValue(uiState.getValue().toBuilder().teachers(teachers).build()));
                    universityDataSource.listSubjectsForDepartment(departmentId)
                            .addOnSuccessListener(subjects -> {
                                List<Subject> withTeacher = new ArrayList<>();
                                for (Subject subject : subjects) {
                                    if (subject.getTeacherUid() != null) withTeacher.add(subject);
                                }
                                uiState.setValue(uiState.getValue().toBuilder().subjects(withTeacher).build());
                            });
                    loadRecords(departmentId, null, null);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<HodTeacherAttendanceReportsUiState> getUiState() {
        return uiState;
    }

    public void onTeacherSelected(@Nullable User teacher) {
        String departmentId = uiState.getValue().getDepartmentId();
        if (departmentId == null) return;
        Subject selectedSubject = uiState.getValue().getSelectedSubject();
        uiState.setValue(uiState.getValue().toBuilder().selectedTeacher(teacher).build());
        loadRecords(departmentId, teacher != null ? teacher.getUid() : null, selectedSubject != null ? selectedSubject.getId() : null);
    }

    public void onSubjectSelected(@Nullable Subject subject) {
        String departmentId = uiState.getValue().getDepartmentId();
        if (departmentId == null) return;
        User selectedTeacher = uiState.getValue().getSelectedTeacher();
        uiState.setValue(uiState.getValue().toBuilder().selectedSubject(subject).build());
        loadRecords(departmentId, selectedTeacher != null ? selectedTeacher.getUid() : null, subject != null ? subject.getId() : null);
    }

    private void loadRecords(String departmentId, @Nullable String teacherId, @Nullable String subjectId) {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).build());
        Task<List<TeacherAttendanceRecord>> task;
        if (subjectId != null) {
            task = teacherAttendanceDataSource.forSubject(subjectId);
        } else if (teacherId != null) {
            task = teacherAttendanceDataSource.forTeacher(teacherId);
        } else {
            task = teacherAttendanceDataSource.forDepartment(departmentId);
        }
        task.addOnSuccessListener(records ->
                        uiState.setValue(uiState.getValue().toBuilder().records(records).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
    }
}
