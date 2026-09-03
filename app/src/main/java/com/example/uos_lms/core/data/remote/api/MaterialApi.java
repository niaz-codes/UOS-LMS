package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateStudyMaterialRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.StudyMaterialEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.StudyMaterialsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateStudyMaterialRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MaterialApi {

    @POST("materials")
    Call<StudyMaterialEnvelopeDto> create(@Body CreateStudyMaterialRequestDto request);

    @GET("materials")
    Call<StudyMaterialsEnvelopeDto> list(@Query("subjectId") String subjectId);

    @PATCH("materials/{id}")
    Call<StudyMaterialEnvelopeDto> update(@Path("id") String id, @Body UpdateStudyMaterialRequestDto request);

    @DELETE("materials/{id}")
    Call<Void> remove(@Path("id") String id);
}
