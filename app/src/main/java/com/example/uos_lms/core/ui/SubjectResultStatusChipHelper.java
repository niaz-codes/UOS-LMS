package com.example.uos_lms.core.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;

/** Colors + labels a TextView as a soft-fill Pass/Fail pill (see AccentColors.applyPill) for
 * one subject row within a semester result. Returns the color used so callers can also apply
 * it to a card's accent bar. */
public final class SubjectResultStatusChipHelper {

    private SubjectResultStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, SubjectResultStatus status) {
        Context context = chip.getContext();
        boolean pass = status == SubjectResultStatus.PASS;
        int colorRes = pass ? R.color.status_success : R.color.error_color;
        AccentColors.applyPill(chip, colorRes, context.getString(pass
                ? R.string.subject_result_status_pass
                : R.string.subject_result_status_fail));
        return colorRes;
    }
}
