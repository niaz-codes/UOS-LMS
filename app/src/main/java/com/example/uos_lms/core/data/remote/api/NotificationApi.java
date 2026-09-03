package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.FcmTokenRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationSettingsDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationSettingsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.UnreadCountResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApi {

    @GET("notifications")
    Call<NotificationsEnvelopeDto> list(@Query("category") String category,
                                         @Query("type") String type,
                                         @Query("read") String read,
                                         @Query("q") String q,
                                         @Query("page") Integer page,
                                         @Query("limit") Integer limit);

    @GET("notifications/unread-count")
    Call<UnreadCountResponseDto> unreadCount();

    @PATCH("notifications/{id}/read")
    Call<NotificationEnvelopeDto> markRead(@Path("id") String id);

    @PATCH("notifications/read-all")
    Call<Void> markAllRead();

    @DELETE("notifications/{id}")
    Call<Void> delete(@Path("id") String id);

    @GET("notifications/settings")
    Call<NotificationSettingsEnvelopeDto> getSettings();

    @PUT("notifications/settings")
    Call<NotificationSettingsEnvelopeDto> updateSettings(@Body NotificationSettingsDto request);

    @POST("notifications/fcm-token")
    Call<Void> registerFcmToken(@Body FcmTokenRequestDto request);

    @HTTP(method = "DELETE", path = "notifications/fcm-token", hasBody = true)
    Call<Void> unregisterFcmToken(@Body FcmTokenRequestDto request);
}
