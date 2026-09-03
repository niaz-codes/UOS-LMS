package com.example.uos_lms.feature.student.presentation.attendance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiLeaveDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.ui.ChartEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StudentAttendanceViewModel extends ViewModel {

    private final ApiUniversityDataSource universityDataSource;
    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiLeaveDataSource leaveDataSource;
    private final AuthApi authApi;
    private final String uid;

    private final MutableLiveData<StudentAttendanceUiState> uiState =
            new MutableLiveData<>(StudentAttendanceUiState.initial());

    private List<Subject> latestSubjects;
    private final Map<String, List<AttendanceRecord>> recordsBySubject = new HashMap<>();

    @Inject
    public StudentAttendanceViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiAttendanceDataSource attendanceDataSource,
            ApiLeaveDataSource leaveDataSource,
            AuthApi authApi,
            SessionManager sessionManager) {
        this.universityDataSource = universityDataSource;
        this.attendanceDataSource = attendanceDataSource;
        this.authApi = authApi;
        CachedSession session = sessionManager.getCachedSession().getValue();
        this.uid = session != null ? session.getUid() : null;
        this.leaveDataSource = leaveDataSource;
        if (this.uid == null) {
            uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage("Session expired. Please log in again.").build());
        } else {
            load();
        }
    }

    public LiveData<StudentAttendanceUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches attendance + approved leaves from the backend without blanking the currently-
     * shown chart - no-ops while a refresh is already in flight (or if there's no session). */
    public void refresh() {
        if (uid == null) return;
        StudentAttendanceUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        load();
    }

    private void load() {
        leaveDataSource.list("APPROVED").addOnSuccessListener(leaves ->
                uiState.setValue(uiState.getValue().toBuilder().approvedLeaves(leaves).build()));

        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null || user.getDepartment() == null || user.getSemester() == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    universityDataSource.mySubjects().addOnSuccessListener(subjects -> {
                        latestSubjects = subjects;
                        List<Task<List<AttendanceRecord>>> tasks = new ArrayList<>();
                        for (Subject subject : subjects) {
                            recordsBySubject.put(subject.getId(), Collections.emptyList());
                            tasks.add(attendanceDataSource.studentAttendanceForSubject(subject.getId(), uid)
                                    .addOnSuccessListener(records -> recordsBySubject.put(subject.getId(), records)));
                        }
                        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    private void recompute() {
        if (latestSubjects == null) return;
        List<Subject> sorted = new ArrayList<>(latestSubjects);
        sorted.sort(Comparator.comparing(Subject::getCode));

        List<ChartEntry> bySubject = new ArrayList<>();
        int totalPresent = 0;
        int totalAll = 0;
        for (Subject subject : sorted) {
            List<AttendanceRecord> records = recordsBySubject.get(subject.getId());
            if (records == null) records = Collections.emptyList();
            int present = 0;
            for (AttendanceRecord record : records) {
                if (record.getStatus() == AttendanceStatus.PRESENT) present++;
            }
            int percentage = records.isEmpty() ? 0 : (present * 100) / records.size();
            bySubject.add(new ChartEntry(subject.getCode(), percentage));
            totalPresent += present;
            totalAll += records.size();
        }
        int overall = totalAll == 0 ? 0 : (totalPresent * 100) / totalAll;

        uiState.setValue(uiState.getValue().toBuilder()
                .overallPercentage(overall)
                .presentCount(totalPresent)
                .totalCount(totalAll)
                .bySubject(bySubject)
                .loading(false)
                .refreshing(false)
                .build());
    }
}
