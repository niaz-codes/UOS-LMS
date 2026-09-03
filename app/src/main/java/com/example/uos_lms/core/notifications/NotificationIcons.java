package com.example.uos_lms.core.notifications;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.NotificationCategory;

/** Small-icon mapping shared by every place that posts or lists a notification for a category:
 * AppNotificationCenter (local tray), FcmService (push tray), and NotificationsFragment
 * (in-app Notification Center row icon). */
public final class NotificationIcons {

    private NotificationIcons() {
    }

    public static int iconFor(NotificationCategory category) {
        if (category == null) return R.drawable.ic_campaign;
        switch (category) {
            case ANNOUNCEMENT:
                return R.drawable.ic_campaign;
            case MESSAGE:
                return R.drawable.ic_chat;
            case EXAM_RESULT:
                return R.drawable.ic_grade;
            case LEAVE:
            case ATTENDANCE:
                return R.drawable.ic_event_busy;
            case ACCOUNT:
                return R.drawable.ic_person;
            case CALENDAR:
                return R.drawable.ic_calendar_month;
            case ACADEMIC_CONTENT:
                return R.drawable.ic_folder_open;
            case PROMOTION:
                return R.drawable.ic_school;
            case SYSTEM:
            default:
                return R.drawable.ic_campaign;
        }
    }
}
