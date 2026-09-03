package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateExamScheduleRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateTimetableSlotRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamScheduleEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamSchedulesEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.TimetableSlotEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.TimetableSlotsEnvelopeDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface SchedulingApi {

    @POST("timetable-slots")
    Call<TimetableSlotEnvelopeDto> createSlot(@Body CreateTimetableSlotRequestDto request);

    @GET("timetable-slots")
    Call<TimetableSlotsEnvelopeDto> listSlots(@QueryMap Map<String, String> filters);

    @DELETE("timetable-slots/{id}")
    Call<Void> removeSlot(@Path("id") String id);

    @POST("exam-schedules")
    Call<ExamScheduleEnvelopeDto> createExamSchedule(@Body CreateExamScheduleRequestDto request);

    @GET("exam-schedules")
    Call<ExamSchedulesEnvelopeDto> listExamSchedules(@QueryMap Map<String, String> filters);

    @POST("exam-schedules/{id}/publish")
    Call<ExamScheduleEnvelopeDto> publish(@Path("id") String id);

    @POST("exam-schedules/{id}/lock")
    Call<ExamScheduleEnvelopeDto> lock(@Path("id") String id);

    @DELETE("exam-schedules/{id}")
    Call<Void> removeExamSchedule(@Path("id") String id);
}
