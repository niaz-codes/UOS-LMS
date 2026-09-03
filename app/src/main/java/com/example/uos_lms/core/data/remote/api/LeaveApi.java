package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ApplyLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.LeaveApplicationEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.LeaveApplicationsEnvelopeDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LeaveApi {

    @POST("leaves")
    Call<LeaveApplicationEnvelopeDto> apply(@Body ApplyLeaveRequestDto request);

    /** Role-scoped server-side: Student sees only their own, Teacher/HOD their department(s),
     * Admin everything - see leaveController.list. */
    @GET("leaves")
    Call<LeaveApplicationsEnvelopeDto> list(@Query("status") String status);

    @POST("leaves/{id}/approve")
    Call<LeaveApplicationEnvelopeDto> approve(@Path("id") String id);

    @POST("leaves/{id}/reject")
    Call<LeaveApplicationEnvelopeDto> reject(@Path("id") String id);
}
