package com.example.uos_lms.feature.hod.presentation.attendance;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.Semester;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HodAttendanceReportsViewModel extends ViewModel {

    private final ApiAttendanceDataSource attendanceDataSource;
    private final ApiUniversityDataSource universityDataSource;
    private final AuthApi authApi;

    private final MutableLiveData<HodAttendanceReportsUiState> uiState =
            new MutableLiveData<>(HodAttendanceReportsUiState.initial());

    @Inject
    public HodAttendanceReportsViewModel(
            ApiAttendanceDataSource attendanceDataSource,
            ApiUniversityDataSource universityDataSource,
            AuthApi authApi) {
        this.attendanceDataSource = attendanceDataSource;
        this.universityDataSource = universityDataSource;
        this.authApi = authApi;
        load();
    }

    public LiveData<HodAttendanceReportsUiState> getUiState() {
        return uiState;
    }

    /** Re-fetches the currently filtered records (same semester filter, if any) without
     * blanking the currently-shown list - no-ops while a refresh is already in flight. */
    public void refresh() {
        HodAttendanceReportsUiState current = uiState.getValue();
        if (current == null || current.isRefreshing()) return;
        String departmentId = current.getDepartmentId();
        if (departmentId == null) return;
        uiState.setValue(current.toBuilder().refreshing(true).errorMessage(null).build());
        Semester selected = current.getSelectedSemester();
        loadRecords(departmentId, selected != null ? selected.getId() : null);
    }

    private void load() {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    String departmentId = user != null ? user.getDepartment() : null;
                    uiState.setValue(uiState.getValue().toBuilder().departmentId(departmentId).build());
                    if (departmentId == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).build());
                        return;
                    }
                    universityDataSource.listSemesters(departmentId)
                            .addOnSuccessListener(semesters -> {
                                List<Semester> sorted = new ArrayList<>(semesters);
                                sorted.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
                                uiState.setValue(uiState.getValue().toBuilder().semesters(sorted).build());
                            });
                    loadRecords(departmentId, null);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
    }

    public void onSemesterSelected(@Nullable Semester semester) {
        String departmentId = uiState.getValue().getDepartmentId();
        if (departmentId == null) return;
        uiState.setValue(uiState.getValue().toBuilder().selectedSemester(semester).build());
        loadRecords(departmentId, semester != null ? semester.getId() : null);
    }

    private void loadRecords(String departmentId, @Nullable String semesterId) {
        uiState.setValue(uiState.getValue().toBuilder().loading(true).build());
        Task<List<AttendanceRecord>> task = semesterId != null
                ? attendanceDataSource.forSemester(departmentId, semesterId)
                : attendanceDataSource.forDepartment(departmentId);
        task.addOnSuccessListener(records ->
                        uiState.setValue(uiState.getValue().toBuilder().records(records).loading(false).refreshing(false).build()))
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).refreshing(false).errorMessage(e.getMessage()).build()));
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
