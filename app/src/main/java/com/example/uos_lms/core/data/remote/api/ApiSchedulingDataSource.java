package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateExamScheduleRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateTimetableSlotRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamScheduleResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.TimetableSlotResponseDto;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.ExamSchedule;
import com.example.uos_lms.core.domain.model.ExamType;
import com.example.uos_lms.core.domain.model.TimetableSlot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreTimetableDataSource + FirestoreExamScheduleDataSource.
 * One-shot only. list()/listExamSchedules() are role-scoped entirely server-side (see backend
 * timetableController/examScheduleController) - every role calls the same methods and each
 * gets the results they're supposed to see. */
@Singleton
public class ApiSchedulingDataSource {

    private final SchedulingApi schedulingApi;

    @Inject
    public ApiSchedulingDataSource(SchedulingApi schedulingApi) {
        this.schedulingApi = schedulingApi;
    }

    public Task<TimetableSlot> createSlot(String subjectId, DayOfWeek day, int startTimeMinutes, int endTimeMinutes, String room) {
        CreateTimetableSlotRequestDto request = CreateTimetableSlotRequestDto.builder()
                .subjectId(subjectId).dayOfWeek(day.name()).startTimeMinutes(startTimeMinutes)
                .endTimeMinutes(endTimeMinutes).room(room).build();
        return RetrofitTasks.call(schedulingApi.createSlot(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSlot().toDomain()));
    }

    public Task<List<TimetableSlot>> slotsForDepartmentSemester(String departmentId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("semesterId", semesterId);
        return listSlots(filters);
    }

    public Task<List<TimetableSlot>> slotsForDepartment(String departmentId) {
        return listSlots(singleFilter("departmentId", departmentId));
    }

    /** Teacher/Student: no filters needed - the backend infers scope from the caller's own role. */
    public Task<List<TimetableSlot>> mySlots() {
        return listSlots(new HashMap<>());
    }

    private Task<List<TimetableSlot>> listSlots(Map<String, String> filters) {
        return RetrofitTasks.call(schedulingApi.listSlots(filters)).onSuccessTask(envelope -> {
            List<TimetableSlot> slots = new ArrayList<>();
            if (envelope.getSlots() != null) {
                for (TimetableSlotResponseDto dto : envelope.getSlots()) slots.add(dto.toDomain());
            }
            return Tasks.forResult(slots);
        });
    }

    public Task<Void> deleteSlot(String slotId) {
        return RetrofitTasks.call(schedulingApi.removeSlot(slotId));
    }

    public Task<ExamSchedule> createExamSchedule(String subjectId, ExamType examType, long examDateMillis,
                                                  int startTimeMinutes, int endTimeMinutes, String room, String invigilatorId) {
        CreateExamScheduleRequestDto request = CreateExamScheduleRequestDto.builder()
                .subjectId(subjectId).examType(examType.name()).examDate(examDateMillis)
                .startTimeMinutes(startTimeMinutes).endTimeMinutes(endTimeMinutes).room(room).invigilatorId(invigilatorId).build();
        return RetrofitTasks.call(schedulingApi.createExamSchedule(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSchedule().toDomain()));
    }

    public Task<List<ExamSchedule>> examSchedulesForDepartment(String departmentId) {
        return listExamSchedules(singleFilter("departmentId", departmentId));
    }

    /** Teacher (own subject or invigilator) / Student (own cohort, published+locked only):
     * no filters needed - the backend infers scope from the caller's own role. */
    public Task<List<ExamSchedule>> myExamSchedules() {
        return listExamSchedules(new HashMap<>());
    }

    private Task<List<ExamSchedule>> listExamSchedules(Map<String, String> filters) {
        return RetrofitTasks.call(schedulingApi.listExamSchedules(filters)).onSuccessTask(envelope -> {
            List<ExamSchedule> schedules = new ArrayList<>();
            if (envelope.getSchedules() != null) {
                for (ExamScheduleResponseDto dto : envelope.getSchedules()) schedules.add(dto.toDomain());
            }
            return Tasks.forResult(schedules);
        });
    }

    public Task<ExamSchedule> publishExamSchedule(String scheduleId) {
        return RetrofitTasks.call(schedulingApi.publish(scheduleId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSchedule().toDomain()));
    }

    public Task<ExamSchedule> lockExamSchedule(String scheduleId) {
        return RetrofitTasks.call(schedulingApi.lock(scheduleId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSchedule().toDomain()));
    }

    public Task<Void> deleteExamSchedule(String scheduleId) {
        return RetrofitTasks.call(schedulingApi.removeExamSchedule(scheduleId));
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }
}
