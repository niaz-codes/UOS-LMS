package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AssignmentEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentSubmissionEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentSubmissionsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateAssignmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.GradeAssignmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SubmitAssignmentRequestDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface AssignmentApi {

    @POST("assignments")
    Call<AssignmentEnvelopeDto> create(@Body CreateAssignmentRequestDto request);

    @GET("assignments")
    Call<AssignmentsEnvelopeDto> list(@QueryMap Map<String, String> filters);

    @DELETE("assignments/{id}")
    Call<Void> remove(@Path("id") String id);

    @POST("assignments/{assignmentId}/submissions")
    Call<AssignmentSubmissionEnvelopeDto> submit(@Path("assignmentId") String assignmentId, @Body SubmitAssignmentRequestDto request);

    @GET("assignments/submissions")
    Call<AssignmentSubmissionsEnvelopeDto> listSubmissions(@QueryMap Map<String, String> filters);

    @PATCH("assignments/submissions/{id}/grade")
    Call<AssignmentSubmissionEnvelopeDto> grade(@Path("id") String id, @Body GradeAssignmentRequestDto request);
}
