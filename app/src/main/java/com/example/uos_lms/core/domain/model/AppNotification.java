package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.Getter;

/** One row in the backend-backed Notification Center. Named AppNotification (not
 * "Notification") to avoid colliding with android.app.Notification. */
@Getter
@Builder(toBuilder = true)
public class AppNotification {
    private final String id;
    private final String type;
    private final NotificationCategory category;
    private final String title;
    private final String body;
    private final String relatedType;
    private final String relatedId;
    private final boolean read;
    private final long createdAtMillis;
}
