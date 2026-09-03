package com.example.uos_lms.feature.teacher.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiTeacherAttendanceDataSource;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherOwnAttendanceViewModel extends ViewModel {

    private final MutableLiveData<TeacherOwnAttendanceUiState> uiState =
            new MutableLiveData<>(TeacherOwnAttendanceUiState.initial());

    @Inject
    public TeacherOwnAttendanceViewModel(ApiTeacherAttendanceDataSource teacherAttendanceDataSource, SessionManager sessionManager) {
        CachedSession session = sessionManager.getCachedSession().getValue();
        String teacherUid = session != null ? session.getUid() : null;
        if (teacherUid == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Session expired. Please log in again.").build());
            return;
        }
        teacherAttendanceDataSource.forTeacher(teacherUid)
                .addOnSuccessListener(records -> uiState.setValue(uiState.getValue().toBuilder().records(records).loading(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    public LiveData<TeacherOwnAttendanceUiState> getUiState() {
        return uiState;
    }
}
