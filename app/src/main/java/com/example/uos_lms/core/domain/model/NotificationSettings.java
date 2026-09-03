package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.Getter;

/** Per-user notification preferences, persisted server-side (backend/src/models/User.js's
 * notificationSettings sub-document) - not device-local like the old NotificationPreferenceManager,
 * so the same preferences follow the account across devices. */
@Getter
@Builder(toBuilder = true)
public class NotificationSettings {
    @Builder.Default
    private final boolean pushEnabled = true;
    @Builder.Default
    private final boolean soundEnabled = true;
    @Builder.Default
    private final boolean vibrationEnabled = true;

    @Builder.Default
    private final boolean account = true;
    @Builder.Default
    private final boolean academicContent = true;
    @Builder.Default
    private final boolean attendance = true;
    @Builder.Default
    private final boolean examResult = true;
    @Builder.Default
    private final boolean leave = true;
    @Builder.Default
    private final boolean announcement = true;
    @Builder.Default
    private final boolean calendar = true;
    @Builder.Default
    private final boolean message = true;
    @Builder.Default
    private final boolean promotion = true;
    @Builder.Default
    private final boolean system = true;

    public boolean isCategoryEnabled(NotificationCategory category) {
        switch (category) {
            case ACCOUNT: return account;
            case ACADEMIC_CONTENT: return academicContent;
            case ATTENDANCE: return attendance;
            case EXAM_RESULT: return examResult;
            case LEAVE: return leave;
            case ANNOUNCEMENT: return announcement;
            case CALENDAR: return calendar;
            case MESSAGE: return message;
            case PROMOTION: return promotion;
            case SYSTEM:
            default: return system;
        }
    }

    public static NotificationSettings initial() {
        return NotificationSettings.builder().build();
    }
}
