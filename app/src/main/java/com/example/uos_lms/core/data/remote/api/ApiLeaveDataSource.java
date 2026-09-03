package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ApplyLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.LeaveApplicationResponseDto;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreLeaveDataSource. One-shot only. list()/status is
 * role-scoped entirely server-side (see backend leaveController) - Student/Teacher/HOD/Admin
 * all call the same method and each get the results they're supposed to see. */
@Singleton
public class ApiLeaveDataSource {

    private final LeaveApi leaveApi;

    @Inject
    public ApiLeaveDataSource(LeaveApi leaveApi) {
        this.leaveApi = leaveApi;
    }

    public Task<LeaveApplication> applyForLeave(long fromDateMillis, long toDateMillis, String reason, String mediaId) {
        ApplyLeaveRequestDto request = ApplyLeaveRequestDto.builder()
                .fromDate(fromDateMillis).toDate(toDateMillis).reason(reason).mediaId(mediaId).build();
        return RetrofitTasks.call(leaveApi.apply(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }

    public Task<List<LeaveApplication>> list(String status) {
        return RetrofitTasks.call(leaveApi.list(status)).onSuccessTask(envelope -> {
            List<LeaveApplication> leaves = new ArrayList<>();
            if (envelope.getLeaves() != null) {
                for (LeaveApplicationResponseDto dto : envelope.getLeaves()) leaves.add(dto.toDomain());
            }
            return Tasks.forResult(leaves);
        });
    }

    public Task<List<LeaveApplication>> all() {
        return list(null);
    }

    public Task<LeaveApplication> approve(String leaveId) {
        return RetrofitTasks.call(leaveApi.approve(leaveId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }

    public Task<LeaveApplication> reject(String leaveId) {
        return RetrofitTasks.call(leaveApi.reject(leaveId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getLeave().toDomain()));
    }
}
