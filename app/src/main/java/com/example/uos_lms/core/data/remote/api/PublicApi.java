package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.DepartmentsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SemestersEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionsEnvelopeDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/** Public read-only curriculum endpoints (backend/src/routes/public.js) used by the registration
 * screen, which runs before the user has a token - the Department/Session/Semester options a
 * sign-up form needs must not sit behind the authenticated + approved-only university router. */
public interface PublicApi {

    @GET("public/departments")
    Call<DepartmentsEnvelopeDto> listDepartments();

    @GET("public/departments/{departmentId}/sessions")
    Call<SessionsEnvelopeDto> listSessions(@Path("departmentId") String departmentId);

    @GET("public/departments/{departmentId}/semesters")
    Call<SemestersEnvelopeDto> listSemesters(@Path("departmentId") String departmentId);
}
