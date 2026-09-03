package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ApplyTeacherLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RejectTeacherLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherLeaveApplicationResponseDto;
import com.example.uos_lms.core.domain.model.LeaveType;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed data source for the Teacher Leave Application system (Teacher applies, HOD
 * decides) - separate from ApiLeaveDataSource, which is the Student Leave Application system
 * (Student applies, any Teacher/HOD decides). See backend/src/models/TeacherLeaveApplication.js
 * for why these stay two independent collections. */
@Singleton
public class ApiTeacherLeaveDataSource {

    private final TeacherLeaveApi teacherLeaveApi;

    @Inject
    public ApiTeacherLeaveDataSource(TeacherLeaveApi teacherLeaveApi) {
        this.teacherLeaveApi = teacherLeaveApi;
    }

    public Task<TeacherLeaveApplication> applyForLeave(LeaveType leaveType, long fromDateMillis, long toDateMillis, String reason) {
        ApplyTeacherLeaveRequestDto request = ApplyTeacherLeaveRequestDto.builder()
                .leaveType(leaveType.name()).fromDate(fromDateMillis).toDate(toDateMillis).reason(reason).build();
        return RetrofitTasks.call(teacherLeaveApi.apply(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }

    public Task<List<TeacherLeaveApplication>> list(String status) {
        return RetrofitTasks.call(teacherLeaveApi.list(status)).onSuccessTask(envelope -> {
            List<TeacherLeaveApplication> leaves = new ArrayList<>();
            if (envelope.getLeaves() != null) {
                for (TeacherLeaveApplicationResponseDto dto : envelope.getLeaves()) leaves.add(dto.toDomain());
            }
            return Tasks.forResult(leaves);
        });
    }

    public Task<List<TeacherLeaveApplication>> all() {
        return list(null);
    }

    public Task<TeacherLeaveApplication> approve(String leaveId) {
        return RetrofitTasks.call(teacherLeaveApi.approve(leaveId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }

    public Task<TeacherLeaveApplication> reject(String leaveId, String reason) {
        RejectTeacherLeaveRequestDto request = RejectTeacherLeaveRequestDto.builder().reason(reason).build();
        return RetrofitTasks.call(teacherLeaveApi.reject(leaveId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }
}
