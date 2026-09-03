package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CountEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateAcademicPlacementRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserDepartmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserIdentifiersRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserProfileRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserRoleRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdateUserStatusRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UserEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UsersEnvelopeDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface UsersApi {

    @GET("users")
    Call<UsersEnvelopeDto> list(@QueryMap Map<String, String> filters);

    @GET("users/counts")
    Call<CountEnvelopeDto> counts(@QueryMap Map<String, String> filters);

    @GET("users/messaging-contacts")
    Call<UsersEnvelopeDto> messagingContacts();

    @GET("users/hod-for-department/{departmentId}")
    Call<UserEnvelopeDto> hodForDepartment(@Path("departmentId") String departmentId);

    @GET("users/{id}")
    Call<UserEnvelopeDto> getById(@Path("id") String id);

    @PATCH("users/{id}/status")
    Call<UserEnvelopeDto> updateStatus(@Path("id") String id, @Body UpdateUserStatusRequestDto request);

    @PATCH("users/{id}/department")
    Call<UserEnvelopeDto> updateDepartment(@Path("id") String id, @Body UpdateUserDepartmentRequestDto request);

    @PATCH("users/{id}/academic")
    Call<UserEnvelopeDto> updateAcademicPlacement(@Path("id") String id, @Body UpdateAcademicPlacementRequestDto request);

    @PATCH("users/{id}/profile")
    Call<UserEnvelopeDto> updateProfile(@Path("id") String id, @Body UpdateUserProfileRequestDto request);

    @PATCH("users/{id}/identifiers")
    Call<UserEnvelopeDto> updateIdentifiers(@Path("id") String id, @Body UpdateUserIdentifiersRequestDto request);

    @PATCH("users/{id}/role")
    Call<UserEnvelopeDto> updateRole(@Path("id") String id, @Body UpdateUserRoleRequestDto request);

    @DELETE("users/{id}")
    Call<Void> remove(@Path("id") String id);
}
