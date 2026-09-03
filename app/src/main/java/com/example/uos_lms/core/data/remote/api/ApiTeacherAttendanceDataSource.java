package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.SaveTeacherAttendanceRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectStatusDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherAttendanceRecordResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherAttendanceRecordsEnvelopeDto;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Course-wise teacher attendance (was this subject's teacher present for THIS course today) -
 * parallel to ApiAttendanceDataSource, which is student attendance in a subject's class session. */
@Singleton
public class ApiTeacherAttendanceDataSource {

    private final TeacherAttendanceApi teacherAttendanceApi;

    @Inject
    public ApiTeacherAttendanceDataSource(TeacherAttendanceApi teacherAttendanceApi) {
        this.teacherAttendanceApi = teacherAttendanceApi;
    }

    public Task<List<TeacherAttendanceRecord>> saveAttendance(String departmentId, String semesterId, String dateKey, List<SubjectStatusDto> records) {
        SaveTeacherAttendanceRequestDto request = SaveTeacherAttendanceRequestDto.builder()
                .departmentId(departmentId).semesterId(semesterId).dateKey(dateKey).records(records).build();
        return mapList(RetrofitTasks.call(teacherAttendanceApi.save(request)));
    }

    public Task<List<TeacherAttendanceRecord>> forSemesterAndDate(String semesterId, String dateKey) {
        Map<String, String> filters = new HashMap<>();
        filters.put("semesterId", semesterId);
        filters.put("dateKey", dateKey);
        return list(filters);
    }

    public Task<List<TeacherAttendanceRecord>> forDepartment(String departmentId) {
        return list(singleFilter("departmentId", departmentId));
    }

    public Task<List<TeacherAttendanceRecord>> forDepartmentAndMonth(String departmentId, String month) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("month", month);
        return list(filters);
    }

    public Task<List<TeacherAttendanceRecord>> forTeacher(String teacherId) {
        return list(singleFilter("teacherId", teacherId));
    }

    public Task<List<TeacherAttendanceRecord>> forSubject(String subjectId) {
        return list(singleFilter("subjectId", subjectId));
    }

    /** Admin-only: every teacher-attendance record system-wide, no filters. */
    public Task<List<TeacherAttendanceRecord>> listAll() {
        return list(new HashMap<>());
    }

    private Task<List<TeacherAttendanceRecord>> list(Map<String, String> filters) {
        return mapList(RetrofitTasks.call(teacherAttendanceApi.list(filters)));
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }

    private static Task<List<TeacherAttendanceRecord>> mapList(Task<TeacherAttendanceRecordsEnvelopeDto> task) {
        return task.onSuccessTask(envelope -> {
            List<TeacherAttendanceRecord> records = new ArrayList<>();
            if (envelope.getRecords() != null) {
                for (TeacherAttendanceRecordResponseDto dto : envelope.getRecords()) records.add(dto.toDomain());
            }
            return Tasks.forResult(records);
        });
    }
}
