package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AttendanceRecordsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SaveAttendanceRequestDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface AttendanceApi {

    @POST("attendance")
    Call<AttendanceRecordsEnvelopeDto> save(@Body SaveAttendanceRequestDto request);

    @GET("attendance")
    Call<AttendanceRecordsEnvelopeDto> list(@QueryMap Map<String, String> filters);
}
