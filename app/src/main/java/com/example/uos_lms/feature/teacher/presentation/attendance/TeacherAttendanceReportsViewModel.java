package com.example.uos_lms.feature.teacher.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherAttendanceReportsViewModel extends ViewModel {

    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final String teacherUid;

    private final MutableLiveData<TeacherAttendanceReportsUiState> uiState =
            new MutableLiveData<>(TeacherAttendanceReportsUiState.initial());

    private final Map<String, List<AttendanceRecord>> recordsBySubject = new HashMap<>();

    @Inject
    public TeacherAttendanceReportsViewModel(
            ApiAttendanceDataSource attendanceDataSource,
            ApiUniversityDataSource universityDataSource,
            SessionManager sessionManager) {
        this.attendanceDataSource = attendanceDataSource;
        this.universityDataSource = universityDataSource;
        CachedSession session = sessionManager.getCachedSession().getValue();
        this.teacherUid = session != null ? session.getUid() : null;
        if (teacherUid == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
            return;
        }
        load();
    }

    public LiveData<TeacherAttendanceReportsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches every subject's attendance history from the backend without blanking the
     * currently-shown list, preserving the user's current subject filter - no-ops while a
     * refresh is already in flight (or if there's no session to fetch with). */
    public void refresh() {
        if (teacherUid == null) return;
        TeacherAttendanceReportsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        universityDataSource.listSubjectsForTeacher(teacherUid)
                .addOnSuccessListener(subjects -> {
                    uiState.setValue(uiState.getValue().toBuilder().subjects(subjects).build());
                    subscribeSubjects(subjects);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void subscribeSubjects(List<Subject> subjects) {
        if (subjects.isEmpty()) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
            return;
        }
        List<com.google.android.gms.tasks.Task<List<AttendanceRecord>>> tasks = new ArrayList<>();
        for (Subject subject : subjects) {
            tasks.add(attendanceDataSource.historyForSubject(subject.getId())
                    .addOnSuccessListener(records -> recordsBySubject.put(subject.getId(), records)));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
    }

    public void onSubjectSelected(Subject subject) {
        uiState.setValue(uiState.getValue().toBuilder().selectedSubject(subject).build());
        recompute();
    }

    private void recompute() {
        Subject selected = uiState.getValue().getSelectedSubject();
        List<AttendanceRecord> combined = new ArrayList<>();
        for (Subject subject : uiState.getValue().getSubjects()) {
            if (selected != null && !selected.getId().equals(subject.getId())) continue;
            List<AttendanceRecord> records = recordsBySubject.get(subject.getId());
            if (records != null) combined.addAll(records);
        }
        uiState.setValue(uiState.getValue().toBuilder().records(combined).loading(false).refreshing(false).build());
    }

    public String buildCsv() {
        StringBuilder sb = new StringBuilder("Student Name,Subject ID,Date,Status");
        for (AttendanceRecord record : uiState.getValue().getRecords()) {
            sb.append('\n')
                    .append(record.getStudentName()).append(',')
                    .append(record.getSubjectId()).append(',')
                    .append(record.getDateKey()).append(',')
                    .append(record.getStatus().name());
        }
        return sb.toString();
    }
}
