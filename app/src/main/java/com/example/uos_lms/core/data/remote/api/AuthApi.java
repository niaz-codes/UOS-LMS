package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AuthResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.ChangePasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ForgotPasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.LoginRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RegisterRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.ResetPasswordRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UpdatePhotoRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.UserEnvelopeDto;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;

import java.util.List;

public interface AuthApi {

    @POST("auth/register")
    Call<UserEnvelopeDto> register(@Body RegisterRequestDto request);

    /** Same backend route as {@link #register}, used only when the user picked a profile photo -
     * multer on the backend only parses multipart bodies, so a photo-less registration still
     * goes through the plain JSON overload above unchanged. `photo` may be null; Retrofit
     * silently omits a null @Part. The academic selections are sent as form fields (single value
     * for STUDENT/HOD, repeated values for the TEACHER's multi-select department list). */
    @Multipart
    @POST("auth/register")
    Call<UserEnvelopeDto> registerWithPhoto(@Part("fullName") RequestBody fullName,
                                             @Part("fatherName") RequestBody fatherName,
                                             @Part("cnic") RequestBody cnic,
                                             @Part("phone") RequestBody phone,
                                             @Part("email") RequestBody email,
                                             @Part("password") RequestBody password,
                                             @Part("role") RequestBody role,
                                             @Part("departmentId") RequestBody departmentId,
                                             @Part("sessionId") RequestBody sessionId,
                                             @Part("currentSemesterId") RequestBody currentSemesterId,
                                             @Part("departmentIds") List<RequestBody> departmentIds,
                                             @Part MultipartBody.Part photo);

    @POST("auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto request);

    @GET("auth/me")
    Call<UserEnvelopeDto> me();

    @PATCH("auth/me/photo")
    Call<UserEnvelopeDto> updateMyPhoto(@Body UpdatePhotoRequestDto request);

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequestDto request);

    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequestDto request);

    @PATCH("auth/me/password")
    Call<Void> changePassword(@Body ChangePasswordRequestDto request);
}
