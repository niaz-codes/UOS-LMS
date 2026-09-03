package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AssignTeacherRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CountEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSemesterRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSessionRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateSubjectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.DepartmentEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.DepartmentsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ReassignSessionStudentsRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SemesterEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SemestersEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.TeacherMyStudentsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateSessionRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateSubjectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UsersEnvelopeDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UniversityApi {

    @GET("departments")
    Call<DepartmentsEnvelopeDto> listDepartments();

    @POST("departments")
    Call<DepartmentEnvelopeDto> createDepartment(@Body CreateDepartmentRequestDto request);

    @PATCH("departments/{id}")
    Call<DepartmentEnvelopeDto> updateDepartment(@Path("id") String id, @Body UpdateDepartmentRequestDto request);

    @DELETE("departments/{id}")
    Call<Void> removeDepartment(@Path("id") String id);

    @GET("departments/{departmentId}/sessions")
    Call<SessionsEnvelopeDto> listSessions(@Path("departmentId") String departmentId);

    @POST("departments/{departmentId}/sessions")
    Call<SessionEnvelopeDto> createSession(@Path("departmentId") String departmentId, @Body CreateSessionRequestDto request);

    @PATCH("sessions/{id}")
    Call<SessionEnvelopeDto> updateSession(@Path("id") String id, @Body UpdateSessionRequestDto request);

    @POST("sessions/{id}/reassign-students")
    Call<Void> reassignSessionStudents(@Path("id") String id, @Body ReassignSessionStudentsRequestDto request);

    @DELETE("sessions/{id}")
    Call<Void> removeSession(@Path("id") String id);

    @GET("sessions/count")
    Call<CountEnvelopeDto> countAllSessions();

    @GET("departments/{departmentId}/semesters")
    Call<SemestersEnvelopeDto> listSemesters(@Path("departmentId") String departmentId);

    @POST("departments/{departmentId}/semesters")
    Call<SemesterEnvelopeDto> createSemester(@Path("departmentId") String departmentId, @Body CreateSemesterRequestDto request);

    @DELETE("semesters/{id}")
    Call<Void> removeSemester(@Path("id") String id);

    @GET("semesters/count")
    Call<CountEnvelopeDto> countAllSemesters();

    @GET("departments/{departmentId}/subjects")
    Call<SubjectsEnvelopeDto> listSubjectsForDepartment(@Path("departmentId") String departmentId);

    @GET("semesters/{semesterId}/subjects")
    Call<SubjectsEnvelopeDto> listSubjectsForSemester(@Path("semesterId") String semesterId);

    @GET("teachers/{teacherId}/subjects")
    Call<SubjectsEnvelopeDto> listSubjectsForTeacher(@Path("teacherId") String teacherId);

    @GET("subjects/mine")
    Call<SubjectsEnvelopeDto> mySubjects();

    @GET("subjects/{subjectId}/roster")
    Call<UsersEnvelopeDto> subjectRoster(@Path("subjectId") String subjectId);

    @GET("teachers/me/students")
    Call<TeacherMyStudentsEnvelopeDto> myStudents();

    @POST("subjects")
    Call<SubjectEnvelopeDto> createSubject(@Body CreateSubjectRequestDto request);

    @PATCH("subjects/{id}")
    Call<SubjectEnvelopeDto> updateSubject(@Path("id") String id, @Body UpdateSubjectRequestDto request);

    @DELETE("subjects/{id}")
    Call<Void> removeSubject(@Path("id") String id);

    @PATCH("subjects/{id}/assign-teacher")
    Call<SubjectEnvelopeDto> assignTeacher(@Path("id") String id, @Body AssignTeacherRequestDto request);
}
