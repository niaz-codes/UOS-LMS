package com.example.uos_lms.feature.notifications;

import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationCategory;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class NotificationsUiState {
    @Builder.Default
    private final List<AppNotification> items = Collections.emptyList();
    private final boolean loading;
    private final boolean refreshing;
    private final boolean loadingMore;
    @Builder.Default
    private final boolean hasMore = true;
    @Builder.Default
    private final int page = 1;
    private final int unreadCount;
    /** null = "All" chip selected. */
    private final NotificationCategory selectedCategory;
    private final boolean unreadOnly;
    private final String searchQuery;
    private final String errorMessage;

    public static NotificationsUiState initial() {
        return NotificationsUiState.builder().loading(true).build();
    }
}
