package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ApplyTeacherLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RejectTeacherLeaveRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherLeaveApplicationEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherLeaveApplicationsEnvelopeDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TeacherLeaveApi {

    @POST("teacher-leaves")
    Call<TeacherLeaveApplicationEnvelopeDto> apply(@Body ApplyTeacherLeaveRequestDto request);

    /** Role-scoped server-side: Teacher sees only their own, HOD their department, Admin
     * everything - see backend teacherLeaveController.list. */
    @GET("teacher-leaves")
    Call<TeacherLeaveApplicationsEnvelopeDto> list(@Query("status") String status);

    @POST("teacher-leaves/{id}/approve")
    Call<TeacherLeaveApplicationEnvelopeDto> approve(@Path("id") String id);

    @POST("teacher-leaves/{id}/reject")
    Call<TeacherLeaveApplicationEnvelopeDto> reject(@Path("id") String id, @Body RejectTeacherLeaveRequestDto request);
}
