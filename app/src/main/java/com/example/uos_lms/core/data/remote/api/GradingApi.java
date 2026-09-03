package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ExamResultEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamResultOptionsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamResultRosterEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamResultsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.PromotionEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.RejectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamCreateRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamMarksRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.StudentSemesterResultsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SubmitResultsRequestDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface GradingApi {

    @POST("results/draft")
    Call<ExamResultsEnvelopeDto> saveDraft(@Body SubmitResultsRequestDto request);

    @POST("results/submit")
    Call<ExamResultsEnvelopeDto> submit(@Body SubmitResultsRequestDto request);

    @POST("results/{id}/approve")
    Call<ExamResultEnvelopeDto> approve(@Path("id") String resultId);

    @POST("results/{id}/reject")
    Call<ExamResultEnvelopeDto> reject(@Path("id") String resultId, @Body RejectRequestDto request);

    @GET("results")
    Call<ExamResultsEnvelopeDto> listResults(@QueryMap Map<String, String> filters);

    @GET("exam-result/options")
    Call<ExamResultOptionsEnvelopeDto> examResultOptions();

    @GET("exam-result/subjects")
    Call<SubjectsEnvelopeDto> examResultSubjects(@QueryMap Map<String, String> filters);

    @GET("exam-result/roster")
    Call<ExamResultRosterEnvelopeDto> examResultRoster(@QueryMap Map<String, String> filters);

    @POST("students/{id}/promote")
    Call<PromotionEnvelopeDto> promote(@Path("id") String studentId);

    @POST("repeat-exams")
    Call<RepeatExamEnvelopeDto> createRepeatExam(@Body RepeatExamCreateRequestDto request);

    @PATCH("repeat-exams/{id}/marks")
    Call<RepeatExamEnvelopeDto> submitRepeatExamMarks(@Path("id") String repeatExamId, @Body RepeatExamMarksRequestDto request);

    @POST("repeat-exams/{id}/approve")
    Call<RepeatExamEnvelopeDto> approveRepeatExam(@Path("id") String repeatExamId);

    @POST("repeat-exams/{id}/reject")
    Call<RepeatExamEnvelopeDto> rejectRepeatExam(@Path("id") String repeatExamId, @Body RejectRequestDto request);

    @GET("repeat-exams")
    Call<RepeatExamsEnvelopeDto> listRepeatExams(@QueryMap Map<String, String> filters);

    @GET("repeat-exams/student/{studentId}")
    Call<RepeatExamsEnvelopeDto> listRepeatExamsForStudent(@Path("studentId") String studentId);

    @GET("semester-results")
    Call<StudentSemesterResultsEnvelopeDto> listSemesterResults(@QueryMap Map<String, String> filters);
}
