package com.example.uos_lms.core.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPage {
    private final List<AppNotification> items;
    private final int total;
    private final int page;
    private final int limit;
    private final int unreadCount;
}
