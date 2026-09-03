package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateQuizRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.GradeQuizAttemptRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizAttemptEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizAttemptsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizzesEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SaveQuizProgressRequestDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface QuizApi {

    @POST("quizzes")
    Call<QuizEnvelopeDto> create(@Body CreateQuizRequestDto request);

    @GET("quizzes")
    Call<QuizzesEnvelopeDto> list(@QueryMap Map<String, String> filters);

    @GET("quizzes/{id}")
    Call<QuizEnvelopeDto> getById(@Path("id") String id);

    @DELETE("quizzes/{id}")
    Call<Void> remove(@Path("id") String id);

    @POST("quizzes/{id}/start")
    Call<QuizAttemptEnvelopeDto> startAttempt(@Path("id") String quizId);

    @PATCH("quizzes/attempts/{id}/answers")
    Call<QuizAttemptEnvelopeDto> saveProgress(@Path("id") String attemptId, @Body SaveQuizProgressRequestDto request);

    @POST("quizzes/{id}/attempts")
    Call<QuizAttemptEnvelopeDto> submitAttempt(@Path("id") String quizId);

    @GET("quizzes/attempts")
    Call<QuizAttemptsEnvelopeDto> listAttempts(@QueryMap Map<String, String> filters);

    @PATCH("quizzes/attempts/{id}/grade")
    Call<QuizAttemptEnvelopeDto> gradeAttempt(@Path("id") String id, @Body GradeQuizAttemptRequestDto request);
}
