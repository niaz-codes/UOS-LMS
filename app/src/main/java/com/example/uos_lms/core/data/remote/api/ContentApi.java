package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AnnouncementEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.AnnouncementsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CalendarEventEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CalendarEventsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateAnnouncementRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateCalendarEventRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ContentApi {

    @POST("announcements")
    Call<AnnouncementEnvelopeDto> createAnnouncement(@Body CreateAnnouncementRequestDto request);

    @GET("announcements")
    Call<AnnouncementsEnvelopeDto> listAnnouncements();

    @DELETE("announcements/{id}")
    Call<Void> deleteAnnouncement(@Path("id") String id);

    @POST("calendar-events")
    Call<CalendarEventEnvelopeDto> createCalendarEvent(@Body CreateCalendarEventRequestDto request);

    @GET("calendar-events")
    Call<CalendarEventsEnvelopeDto> listCalendarEvents();

    @DELETE("calendar-events/{id}")
    Call<Void> deleteCalendarEvent(@Path("id") String id);
}
