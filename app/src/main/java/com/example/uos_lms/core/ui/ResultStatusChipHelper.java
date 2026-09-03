package com.example.uos_lms.core.ui;

import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.ResultStatus;

/** Colors + labels a TextView as a soft-fill status pill (see AccentColors.applyPill / the
 * UI_UX_Design/ reference) - reused by every exam-result list (Admin/HOD monitors, HOD
 * approvals). Returns the color used so callers can also apply it to a card's accent bar. */
public final class ResultStatusChipHelper {

    private ResultStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, ResultStatus status) {
        int colorRes;
        switch (status) {
            case APPROVED:
                colorRes = R.color.status_success;
                break;
            case PENDING_HOD_APPROVAL:
                colorRes = R.color.status_warning;
                break;
            case REJECTED:
                colorRes = R.color.error_color;
                break;
            case DRAFT:
            default:
                colorRes = R.color.on_surface_variant_color;
                break;
        }
        AccentColors.applyPill(chip, colorRes, status.name());
        return colorRes;
    }
}
