package com.example.uos_lms.core.ui;

import android.content.Context;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

/** Attaches a small unread-count badge to the bell/notifications icon on each role dashboard's
 * toolbar - shared by all four (Admin/HOD/Teacher/Student) since they all expose the same
 * R.id.actionNotifications menu item. Backed by AppNotificationCenter.getUnreadCount(). */
public final class NotificationBadgeHelper {

    private NotificationBadgeHelper() {
    }

    public static BadgeDrawable attach(Context context, MaterialToolbar toolbar, int menuItemId) {
        BadgeDrawable badge = BadgeDrawable.create(context);
        badge.setMaxCharacterCount(2);
        BadgeUtils.attachBadgeDrawable(badge, toolbar, menuItemId);
        return badge;
    }

    public static void update(BadgeDrawable badge, int unreadCount) {
        badge.setVisible(unreadCount > 0);
        if (unreadCount > 0) {
            badge.setNumber(unreadCount);
        }
    }
}
