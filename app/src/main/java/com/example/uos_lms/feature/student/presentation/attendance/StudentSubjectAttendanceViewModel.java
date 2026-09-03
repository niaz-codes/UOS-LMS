package com.example.uos_lms.feature.student.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentSubjectAttendanceViewModel extends ViewModel {

    private final ApiAttendanceDataSource attendanceDataSource;
    private final String subjectId;
    private final String uid;

    private final MutableLiveData<StudentSubjectAttendanceUiState> uiState =
            new MutableLiveData<>(StudentSubjectAttendanceUiState.initial());

    @Inject
    public StudentSubjectAttendanceViewModel(
            SavedStateHandle savedStateHandle,
            ApiAttendanceDataSource attendanceDataSource,
            SessionManager sessionManager) {
        this.attendanceDataSource = attendanceDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
        CachedSession session = sessionManager.getCachedSession().getValue();
        this.uid = session != null ? session.getUid() : null;
        if (uid == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Session expired. Please log in again.").build());
            return;
        }
        load();
    }

    public LiveData<StudentSubjectAttendanceUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches this subject's attendance from the backend without blanking the currently-
     * shown list - no-ops while a refresh is already in flight (or if there's no session). */
    public void refresh() {
        if (uid == null) return;
        StudentSubjectAttendanceUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        attendanceDataSource.studentAttendanceForSubject(subjectId, uid)
                .addOnSuccessListener(records -> {
                    List<AttendanceRecord> sorted = new ArrayList<>(records);
                    sorted.sort(Comparator.comparingLong(AttendanceRecord::getDateMillis).reversed());
                    int present = 0;
                    for (AttendanceRecord record : sorted) {
                        if (record.getStatus() == AttendanceStatus.PRESENT) present++;
                    }
                    uiState.setValue(uiState.getValue().toBuilder()
                            .records(sorted)
                            .presentCount(present)
                            .totalCount(sorted.size())
                            .loading(false)
                            .refreshing(false)
                            .build());
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }
}
