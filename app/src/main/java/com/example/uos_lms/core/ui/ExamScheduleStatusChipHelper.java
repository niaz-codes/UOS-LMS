package com.example.uos_lms.core.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.ExamScheduleStatus;

/** Colors + labels a TextView as a soft-fill status pill (see AccentColors.applyPill) - reused
 * by every role's Exam Schedule screen (was duplicated per-Fragment before). Returns the color
 * used so callers can also apply it to a card's accent bar. */
public final class ExamScheduleStatusChipHelper {

    private ExamScheduleStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, ExamScheduleStatus status) {
        Context context = chip.getContext();
        int colorRes;
        int textRes;
        if (status == ExamScheduleStatus.PUBLISHED) {
            colorRes = R.color.status_success;
            textRes = R.string.exam_status_published;
        } else if (status == ExamScheduleStatus.LOCKED) {
            colorRes = R.color.status_info;
            textRes = R.string.exam_status_locked;
        } else {
            colorRes = R.color.on_surface_variant_color;
            textRes = R.string.exam_status_draft;
        }
        AccentColors.applyPill(chip, colorRes, context.getString(textRes));
        return colorRes;
    }
}
