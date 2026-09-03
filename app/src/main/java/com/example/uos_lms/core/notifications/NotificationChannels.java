package com.example.uos_lms.core.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.NotificationCategory;

import java.util.Locale;

/** One notification channel per NotificationCategory - lets Android 8+ users control sound/
 * vibration/importance per category from system settings (per-notification Builder overrides
 * are ignored on API 26+, a platform restriction, so real per-category control has to happen at
 * the channel level). Channel ids ("channel_" + lowercase category) match exactly what the
 * backend's services/fcm.js computes for a push's `android.notification.channelId`, so a push
 * and a locally-polled notification for the same category always land in the same channel. */
public final class NotificationChannels {

    private static final String CHANNEL_PREFIX = "channel_";

    private NotificationChannels() {
    }

    public static String channelIdFor(@Nullable NotificationCategory category) {
        return CHANNEL_PREFIX + (category != null ? category.name() : NotificationCategory.SYSTEM.name()).toLowerCase(Locale.US);
    }

    public static NotificationCategory parseCategory(@Nullable String raw) {
        if (raw == null) return NotificationCategory.SYSTEM;
        try {
            return NotificationCategory.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return NotificationCategory.SYSTEM;
        }
    }

    public static void createAll(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        for (NotificationCategory category : NotificationCategory.values()) {
            int importance = category == NotificationCategory.SYSTEM
                    ? NotificationManager.IMPORTANCE_DEFAULT
                    : NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelIdFor(category), displayNameFor(context, category), importance);
            manager.createNotificationChannel(channel);
        }
    }

    private static String displayNameFor(Context context, NotificationCategory category) {
        switch (category) {
            case ACCOUNT:
                return context.getString(R.string.category_account);
            case ACADEMIC_CONTENT:
                return context.getString(R.string.category_academic_content);
            case ATTENDANCE:
                return context.getString(R.string.category_attendance);
            case EXAM_RESULT:
                return context.getString(R.string.category_exam_result);
            case LEAVE:
                return context.getString(R.string.category_leave);
            case ANNOUNCEMENT:
                return context.getString(R.string.category_announcement);
            case CALENDAR:
                return context.getString(R.string.category_calendar);
            case MESSAGE:
                return context.getString(R.string.category_message);
            case PROMOTION:
                return context.getString(R.string.category_promotion);
            case SYSTEM:
            default:
                return context.getString(R.string.category_system);
        }
    }
}
