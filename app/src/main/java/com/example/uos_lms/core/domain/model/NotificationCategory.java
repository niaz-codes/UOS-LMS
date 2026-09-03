package com.example.uos_lms.core.domain.model;

/** Mirrors backend/src/constants/enums.js's NotificationCategory exactly - used both for the
 * Notification Center's filter chips and the Notification Settings screen's per-category toggles. */
public enum NotificationCategory {
    ACCOUNT,
    ACADEMIC_CONTENT,
    ATTENDANCE,
    EXAM_RESULT,
    LEAVE,
    ANNOUNCEMENT,
    CALENDAR,
    MESSAGE,
    PROMOTION,
    SYSTEM,
}
