package com.example.uos_lms.core.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.NotificationCategory;
import com.example.uos_lms.core.domain.model.UserRole;

/** Shared helpers for the accent-bar-card / soft-pill-badge visual language (see
 * UI_UX_Design/ reference): a colored left/top accent bar on an otherwise-neutral card, and a
 * soft-filled, colored-outline status pill. Both take a plain {@code @ColorRes int} so callers
 * can mix role colors (role_admin_start, etc.) and status colors (status_success, etc.)
 * interchangeably - there's no fixed enum, any color resource works. */
public final class AccentColors {

    private AccentColors() {
    }

    /** Tints an accent-bar View (its background must already be bg_accent_bar_left or
     * bg_accent_bar_top in XML) to the given color. Safe to call on a recycled RecyclerView
     * row - setBackgroundTintList doesn't require a fresh/mutated Drawable per view. */
    public static void applyBar(View barView, @ColorRes int colorRes) {
        ViewCompat.setBackgroundTintList(barView, android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(barView.getContext(), colorRes)));
    }

    /** Sets a status pill's text, text color, and soft-fill/outline background in one call.
     * Mutates its own copy of bg_status_pill so recycled rows never bleed color into each
     * other. */
    public static void applyPill(TextView pillView, @ColorRes int colorRes, CharSequence text) {
        Context context = pillView.getContext();
        int color = ContextCompat.getColor(context, colorRes);
        pillView.setText(text);
        pillView.setTextColor(color);

        Drawable background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill);
        if (background == null) return;
        background = background.mutate();
        if (background instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gradient = (android.graphics.drawable.GradientDrawable) background;
            gradient.setColor(withAlpha(color, 0.16f));
            gradient.setStroke(dpToPx(context, 1), color);
        }
        pillView.setBackground(background);
    }

    /** Same color at a lower alpha - a soft circular badge fill behind a full-saturation icon/
     * letter (e.g. a grade badge), reusing a status/role color without needing a dedicated
     * "container" resource for every one of them. */
    public static int withAlpha(int color, float alphaFraction) {
        int alpha = Math.round(255 * alphaFraction);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /** Role -> brand color (see UI_UX_Design/ reference: Admin=red, HOD=purple, Teacher=blue,
     * Student=green). Used to tint a user row's accent bar / role badge by the ROLE OF THE
     * LISTED USER, independent of which role is viewing the screen. */
    @ColorRes
    public static int colorForRole(UserRole role) {
        if (role == null) return R.color.status_info;
        switch (role) {
            case ADMIN:
                return R.color.role_admin_start;
            case HOD:
                return R.color.role_hod_start;
            case TEACHER:
                return R.color.role_teacher_start;
            case STUDENT:
            default:
                return R.color.role_student_start;
        }
    }

    /** HEC-style letter grade -> semantic color tier (A/A+ = success, B+/B = info, C+/C/D =
     * warning, F/anything else = error). Matches services/gradeScale.js's 11-band scale on the
     * backend, just collapsed to 4 visual tiers. */
    @ColorRes
    public static int colorForGrade(String grade) {
        if (grade == null) return R.color.status_info;
        switch (grade) {
            case "A+":
            case "A":
                return R.color.status_success;
            case "B+":
            case "B":
                return R.color.status_info;
            case "C+":
            case "C":
            case "D":
                return R.color.status_warning;
            case "F":
                return R.color.error_color;
            default:
                return R.color.status_info;
        }
    }

    /** NotificationCategory -> accent color, for item_notification_row.xml's accent bar. */
    @ColorRes
    public static int colorForNotificationCategory(NotificationCategory category) {
        if (category == null) return R.color.on_surface_variant_color;
        switch (category) {
            case ANNOUNCEMENT:
                return R.color.role_admin_start;
            case MESSAGE:
                return R.color.status_info;
            case EXAM_RESULT:
                return R.color.status_success;
            case LEAVE:
            case ATTENDANCE:
                return R.color.status_warning;
            case CALENDAR:
                return R.color.status_info;
            case ACADEMIC_CONTENT:
                return R.color.role_teacher_start;
            case PROMOTION:
                return R.color.role_hod_start;
            case ACCOUNT:
            case SYSTEM:
            default:
                return R.color.on_surface_variant_color;
        }
    }
}
