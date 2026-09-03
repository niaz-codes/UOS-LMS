package com.example.uos_lms.feature.teacher.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.dto.StudentStatusDto;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MarkAttendanceViewModel extends ViewModel {

    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final String teacherUid;
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String dateKey;

    private final MutableLiveData<MarkAttendanceUiState> uiState = new MutableLiveData<>(MarkAttendanceUiState.initial());
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);

    @Inject
    public MarkAttendanceViewModel(
            SavedStateHandle savedStateHandle,
            ApiAttendanceDataSource attendanceDataSource,
            ApiUniversityDataSource universityDataSource,
            SessionManager sessionManager) {
        this.attendanceDataSource = attendanceDataSource;
        this.universityDataSource = universityDataSource;
        CachedSession session = sessionManager.getCachedSession().getValue();
        this.teacherUid = session != null ? session.getUid() : null;
        this.subjectId = savedStateHandle.get("subjectId");
        this.departmentId = savedStateHandle.get("departmentId");
        this.semesterId = savedStateHandle.get("semesterId");
        this.dateKey = savedStateHandle.get("dateKey");
        uiState.setValue(uiState.getValue().toBuilder().dateLabel(DateKeyUtils.dateKeyToDisplay(dateKey)).build());
        load();
    }

    public LiveData<MarkAttendanceUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    private void load() {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).errorMessage(null).build());
        Tasks.whenAllSuccess(
                universityDataSource.subjectRoster(subjectId),
                attendanceDataSource.sessionAttendance(subjectId, dateKey)
        ).addOnSuccessListener(results -> {
            //noinspection unchecked
            List<User> students = (List<User>) results.get(0);
            //noinspection unchecked
            List<AttendanceRecord> records = (List<AttendanceRecord>) results.get(1);
            recombine(students, records);
        }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private void recombine(List<User> students, List<AttendanceRecord> records) {
        List<RosterRow> rows = new ArrayList<>();
        List<User> sorted = new ArrayList<>(students);
        sorted.sort((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()));
        for (User student : sorted) {
            AttendanceStatus status = AttendanceStatus.PRESENT;
            for (AttendanceRecord record : records) {
                if (record.getStudentUid().equals(student.getUid())) {
                    status = record.getStatus();
                    break;
                }
            }
            rows.add(RosterRow.builder().student(student).status(status).build());
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).loading(false).build());
    }

    public void toggleStatus(String studentUid) {
        List<RosterRow> rows = new ArrayList<>();
        for (RosterRow row : uiState.getValue().getRows()) {
            if (row.getStudent().getUid().equals(studentUid)) {
                AttendanceStatus next = row.getStatus() == AttendanceStatus.PRESENT ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT;
                rows.add(row.toBuilder().status(next).build());
            } else {
                rows.add(row);
            }
        }
        uiState.setValue(uiState.getValue().toBuilder().rows(rows).build());
    }

    public void save() {
        if (teacherUid == null) {
            uiState.setValue(uiState.getValue().toBuilder().errorMessage("Session expired. Please log in again.").build());
            return;
        }
        List<StudentStatusDto> records = new ArrayList<>();
        for (RosterRow row : uiState.getValue().getRows()) {
            records.add(new StudentStatusDto(row.getStudent().getUid(), row.getStatus().name()));
        }
        uiState.setValue(uiState.getValue().toBuilder().saving(true).errorMessage(null).build());
        attendanceDataSource.saveAttendance(subjectId, departmentId, semesterId, dateKey, records)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).build());
                    saved.setValue(true);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().saving(false).errorMessage(e.getMessage()).build()));
    }
}
