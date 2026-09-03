package com.example.uos_lms.feature.notifications.fcm;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.uos_lms.core.data.remote.api.ApiNotificationDataSource;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.notifications.NotificationChannels;
import com.example.uos_lms.core.notifications.NotificationIcons;
import com.example.uos_lms.core.notifications.NotificationRouter;
import com.example.uos_lms.core.session.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Delivers push notifications while the app is backgrounded or killed - the complement to
 * AppNotificationCenter's foreground/backgrounded-but-alive polling. The backend
 * (services/fcm.js) sends a data-only payload (deliberately no top-level `notification` block)
 * so onMessageReceived always fires, in every app state, letting the app fully control channel
 * routing, grouping, and tap-to-open here rather than the OS building a generic tray entry. */
@AndroidEntryPoint
public class FcmService extends FirebaseMessagingService {

    @Inject
    ApiNotificationDataSource notificationDataSource;
    @Inject
    SessionManager sessionManager;

    /** Fires whenever FCM (re)issues this device's registration token - on first install, after
     * data is cleared, or periodically per Google's own rotation. If nobody's signed in yet,
     * do nothing here; login itself registers the current token (see LoginViewModel). */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        if (sessionManager.getAuthToken() == null) return;
        notificationDataSource.registerFcmToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        String title = data.get("title");
        if (title == null || title.isEmpty()) return;
        String body = data.get("body");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCategory category = NotificationChannels.parseCategory(data.get("category"));
        String relatedId = data.get("relatedId");
        int notificationKey = relatedId != null ? relatedId.hashCode() : (int) System.currentTimeMillis();
        String channelId = NotificationChannels.channelIdFor(category);

        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationKey,
                NotificationRouter.buildLaunchIntent(this, category),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(NotificationIcons.iconFor(category))
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(contentIntent)
                .setGroup(channelId)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat.from(this).notify(notificationKey, notification);
    }
}
