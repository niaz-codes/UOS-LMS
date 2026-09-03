package com.example.uos_lms.core.ui;

import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.AttendanceStatus;

/** Colors + labels a TextView as a soft-fill status pill (see AccentColors.applyPill) - reused
 * by every role's Attendance list (was duplicated per-Fragment before). Returns the color used
 * so callers can also apply it to a card's accent bar in one call. */
public final class AttendanceStatusChipHelper {

    private AttendanceStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, AttendanceStatus status) {
        boolean present = status == AttendanceStatus.PRESENT;
        int colorRes = present ? R.color.status_success : R.color.error_color;
        AccentColors.applyPill(chip, colorRes, chip.getContext().getString(present ? R.string.present : R.string.absent));
        return colorRes;
    }
}
