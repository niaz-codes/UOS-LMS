package com.example.uos_lms.feature.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.uos_lms.core.data.remote.api.ApiNotificationDataSource;
import com.example.uos_lms.core.domain.model.AppNotification;
import com.example.uos_lms.core.domain.model.NotificationPage;
import com.example.uos_lms.core.domain.model.NotificationSettings;
import com.example.uos_lms.core.notifications.NotificationChannels;
import com.example.uos_lms.core.notifications.NotificationIcons;
import com.example.uos_lms.core.notifications.NotificationRouter;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Session-scoped: polls the backend's unified /api/notifications endpoint every
 * POLL_INTERVAL_MS while a session is active (started after login, stopped on logout) and posts
 * a local OS notification for any unread item it hasn't seen before - the foreground/backgrounded
 * -but-alive complement to FCM (which, once configured, additionally covers the killed-app case;
 * see feature/notifications/fcm). The first poll only seeds "already there" so login doesn't fire
 * a notification for every historical unread item. Also exposes unread count for the toolbar
 * bell-icon badge on each role's dashboard.
 */
@Singleton
public class AppNotificationCenter {

    private static final long POLL_INTERVAL_MS = 30_000L;
    private static final int PAGE_SIZE = 50;

    private final ApiNotificationDataSource notificationDataSource;
    private final Context appContext;

    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::poll;
    private final Set<String> seenIds = new HashSet<>();
    private boolean seededOnce;
    private boolean running;
    private NotificationSettings cachedSettings = NotificationSettings.initial();

    @Inject
    public AppNotificationCenter(ApiNotificationDataSource notificationDataSource, @ApplicationContext Context appContext) {
        this.notificationDataSource = notificationDataSource;
        this.appContext = appContext;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public void start() {
        stop();
        running = true;
        notificationDataSource.getSettings().addOnSuccessListener(settings -> cachedSettings = settings);
        poll();
    }

    public void stop() {
        running = false;
        pollHandler.removeCallbacks(pollRunnable);
        seenIds.clear();
        seededOnce = false;
        unreadCount.setValue(0);
    }

    private void poll() {
        notificationDataSource.list(null, true, null, 1, PAGE_SIZE)
                .addOnSuccessListener(this::onResult)
                .addOnCompleteListener(task -> {
                    if (running) pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                });
    }

    private void onResult(NotificationPage page) {
        unreadCount.setValue(page.getUnreadCount());

        Set<String> currentIds = new HashSet<>();
        for (AppNotification item : page.getItems()) {
            currentIds.add(item.getId());
            if (seededOnce && !seenIds.contains(item.getId())) {
                postLocalNotification(item);
            }
        }
        seenIds.clear();
        seenIds.addAll(currentIds);
        seededOnce = true;
    }

    private void postLocalNotification(AppNotification item) {
        if (!cachedSettings.isPushEnabled() || !cachedSettings.isCategoryEnabled(item.getCategory())) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String channelId = NotificationChannels.channelIdFor(item.getCategory());
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, item.getId().hashCode(),
                NotificationRouter.buildLaunchIntent(appContext, item.getCategory()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // setSound()/setVibrate() below only take effect on API < 26 - on Oreo+ every aspect of
        // sound/vibration is governed by the notification channel itself (a platform restriction,
        // not something an app can override per-notification), which is why the Notification
        // Settings screen also offers a shortcut into the system's per-app channel settings.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(NotificationIcons.iconFor(item.getCategory()))
                .setContentTitle(item.getTitle())
                .setContentText(item.getBody())
                .setContentIntent(contentIntent)
                .setGroup(channelId)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (!cachedSettings.isSoundEnabled()) builder.setSound(null);
        if (!cachedSettings.isVibrationEnabled()) builder.setVibrate(new long[]{0});

        Notification notification = builder.build();
        NotificationManagerCompat.from(appContext).notify(item.getId().hashCode(), notification);
    }
}
