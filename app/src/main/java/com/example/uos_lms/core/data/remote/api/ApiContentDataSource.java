package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AnnouncementResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.CalendarEventResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateAnnouncementRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateCalendarEventRequestDto;
import com.example.uos_lms.core.domain.model.Announcement;
import com.example.uos_lms.core.domain.model.CalendarEvent;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreAnnouncementDataSource + FirestoreCalendarDataSource
 * (both single shared screens, so combined here rather than split). One-shot only. */
@Singleton
public class ApiContentDataSource {

    private final ContentApi contentApi;

    @Inject
    public ApiContentDataSource(ContentApi contentApi) {
        this.contentApi = contentApi;
    }

    public Task<Announcement> createAnnouncement(String title, String body, String scope, String departmentId, String subjectId) {
        CreateAnnouncementRequestDto request = CreateAnnouncementRequestDto.builder()
                .title(title).body(body).scope(scope).departmentId(departmentId).subjectId(subjectId).build();
        return RetrofitTasks.call(contentApi.createAnnouncement(request))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getAnnouncement().toDomain()));
    }

    public Task<List<Announcement>> listAnnouncements() {
        return RetrofitTasks.call(contentApi.listAnnouncements()).onSuccessTask(envelope -> {
            List<Announcement> announcements = new ArrayList<>();
            for (AnnouncementResponseDto dto : envelope.getAnnouncements()) announcements.add(dto.toDomain());
            return Tasks.forResult(announcements);
        });
    }

    public Task<Void> deleteAnnouncement(String id) {
        return RetrofitTasks.call(contentApi.deleteAnnouncement(id)).onSuccessTask(v -> Tasks.forResult(null));
    }

    public Task<CalendarEvent> createCalendarEvent(String title, String description, String type, long dateMillis) {
        CreateCalendarEventRequestDto request = CreateCalendarEventRequestDto.builder()
                .title(title).description(description).type(type).date(dateMillis).build();
        return RetrofitTasks.call(contentApi.createCalendarEvent(request))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getEvent().toDomain()));
    }

    public Task<List<CalendarEvent>> listCalendarEvents() {
        return RetrofitTasks.call(contentApi.listCalendarEvents()).onSuccessTask(envelope -> {
            List<CalendarEvent> events = new ArrayList<>();
            for (CalendarEventResponseDto dto : envelope.getEvents()) events.add(dto.toDomain());
            return Tasks.forResult(events);
        });
    }

    public Task<Void> deleteCalendarEvent(String id) {
        return RetrofitTasks.call(contentApi.deleteCalendarEvent(id)).onSuccessTask(v -> Tasks.forResult(null));
    }
}
