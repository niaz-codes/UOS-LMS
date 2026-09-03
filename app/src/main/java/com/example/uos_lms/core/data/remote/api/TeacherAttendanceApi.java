package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.SaveTeacherAttendanceRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherAttendanceRecordsEnvelopeDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface TeacherAttendanceApi {

    @POST("teacher-attendance")
    Call<TeacherAttendanceRecordsEnvelopeDto> save(@Body SaveTeacherAttendanceRequestDto request);

    @GET("teacher-attendance")
    Call<TeacherAttendanceRecordsEnvelopeDto> list(@QueryMap Map<String, String> filters);
}
