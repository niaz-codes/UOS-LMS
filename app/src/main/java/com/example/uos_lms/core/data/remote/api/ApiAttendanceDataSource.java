package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AttendanceRecordResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.AttendanceRecordsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SaveAttendanceRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.StudentStatusDto;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreAttendanceDataSource. One-shot only. */
@Singleton
public class ApiAttendanceDataSource {

    private final AttendanceApi attendanceApi;

    @Inject
    public ApiAttendanceDataSource(AttendanceApi attendanceApi) {
        this.attendanceApi = attendanceApi;
    }

    public Task<List<AttendanceRecord>> saveAttendance(String subjectId, String departmentId, String semesterId,
                                                         String dateKey, List<StudentStatusDto> records) {
        SaveAttendanceRequestDto request = SaveAttendanceRequestDto.builder()
                .subjectId(subjectId).departmentId(departmentId).semesterId(semesterId).dateKey(dateKey).records(records).build();
        return mapList(RetrofitTasks.call(attendanceApi.save(request)));
    }

    public Task<List<AttendanceRecord>> sessionAttendance(String subjectId, String dateKey) {
        Map<String, String> filters = new HashMap<>();
        filters.put("subjectId", subjectId);
        filters.put("dateKey", dateKey);
        return list(filters);
    }

    public Task<List<AttendanceRecord>> historyForSubject(String subjectId) {
        return list(singleFilter("subjectId", subjectId));
    }

    public Task<List<AttendanceRecord>> studentAttendanceForSubject(String subjectId, String studentId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("subjectId", subjectId);
        filters.put("studentId", studentId);
        return list(filters);
    }

    public Task<List<AttendanceRecord>> forDepartment(String departmentId) {
        return list(singleFilter("departmentId", departmentId));
    }

    public Task<List<AttendanceRecord>> forSemester(String departmentId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("semesterId", semesterId);
        return list(filters);
    }

    private Task<List<AttendanceRecord>> list(Map<String, String> filters) {
        return mapList(RetrofitTasks.call(attendanceApi.list(filters)));
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }

    private static Task<List<AttendanceRecord>> mapList(Task<AttendanceRecordsEnvelopeDto> task) {
        return task.onSuccessTask(envelope -> {
            List<AttendanceRecord> records = new ArrayList<>();
            if (envelope.getRecords() != null) {
                for (AttendanceRecordResponseDto dto : envelope.getRecords()) records.add(dto.toDomain());
            }
            return Tasks.forResult(records);
        });
    }
}
