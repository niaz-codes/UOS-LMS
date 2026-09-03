package com.example.uos_lms.core.data.remote.api;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.api.dto.FcmTokenRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.NotificationSettingsDto;
import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.domain.model.NotificationPage;
import com.example.uos_lms.core.domain.model.NotificationSettings;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Notification Center + Notification Settings + FCM token registration, all backed by
 * backend/src/routes/notifications.js. Replaces AppNotificationCenter's client-side polling
 * aggregation with a real server-side history the backend writes to as things happen (see
 * services/notificationService.js on the backend). */
@Singleton
public class ApiNotificationDataSource {

    private final NotificationApi api;

    @Inject
    public ApiNotificationDataSource(NotificationApi api) {
        this.api = api;
    }

    public Task<NotificationPage> list(@Nullable NotificationCategory category, @Nullable Boolean read,
                                        @Nullable String query, int page, int limit) {
        String categoryParam = category != null ? category.name() : null;
        String readParam = read != null ? String.valueOf(read) : null;
        return RetrofitTasks.call(api.list(categoryParam, null, readParam, query, page, limit))
                .onSuccessTask(envelope -> {
                    List<AppNotification> items = new ArrayList<>();
                    for (NotificationResponseDto dto : envelope.getNotifications()) items.add(dto.toDomain());
                    return Tasks.forResult(NotificationPage.builder()
                            .items(items)
                            .total(envelope.getTotal())
                            .page(envelope.getPage())
                            .limit(envelope.getLimit())
                            .unreadCount(envelope.getUnreadCount())
                            .build());
                });
    }

    public Task<Integer> unreadCount() {
        return RetrofitTasks.call(api.unreadCount()).onSuccessTask(dto -> Tasks.forResult(dto.getUnreadCount()));
    }

    public Task<Void> markRead(String id) {
        return RetrofitTasks.call(api.markRead(id)).onSuccessTask(v -> Tasks.forResult(null));
    }

    public Task<Void> markAllRead() {
        return RetrofitTasks.call(api.markAllRead());
    }

    public Task<Void> delete(String id) {
        return RetrofitTasks.call(api.delete(id));
    }

    public Task<NotificationSettings> getSettings() {
        return RetrofitTasks.call(api.getSettings()).onSuccessTask(envelope -> Tasks.forResult(envelope.getSettings().toDomain()));
    }

    public Task<NotificationSettings> updateSettings(NotificationSettings settings) {
        return RetrofitTasks.call(api.updateSettings(NotificationSettingsDto.fromDomain(settings)))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getSettings().toDomain()));
    }

    public Task<Void> registerFcmToken(String token) {
        return RetrofitTasks.call(api.registerFcmToken(new FcmTokenRequestDto(token)));
    }

    public Task<Void> unregisterFcmToken(String token) {
        return RetrofitTasks.call(api.unregisterFcmToken(new FcmTokenRequestDto(token)));
    }
}
