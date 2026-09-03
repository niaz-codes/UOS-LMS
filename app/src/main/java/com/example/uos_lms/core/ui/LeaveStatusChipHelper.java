package com.example.uos_lms.core.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.LeaveStatus;

/** Colors + labels a TextView as a soft-fill status pill (see AccentColors.applyPill) - reused
 * by every role's Leave screen (was duplicated per-Fragment before). Returns the color used so
 * callers can also apply it to a card's accent bar in one call. */
public final class LeaveStatusChipHelper {

    private LeaveStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, LeaveStatus status) {
        Context context = chip.getContext();
        int colorRes;
        int textRes;
        if (status == LeaveStatus.APPROVED) {
            colorRes = R.color.status_success;
            textRes = R.string.leave_status_approved;
        } else if (status == LeaveStatus.REJECTED) {
            colorRes = R.color.error_color;
            textRes = R.string.leave_status_rejected;
        } else {
            colorRes = R.color.status_warning;
            textRes = R.string.leave_status_pending;
        }
        AccentColors.applyPill(chip, colorRes, context.getString(textRes));
        return colorRes;
    }
}
