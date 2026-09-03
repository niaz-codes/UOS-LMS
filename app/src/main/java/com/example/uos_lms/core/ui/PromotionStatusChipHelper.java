package com.example.uos_lms.core.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.PromotionStatus;

/** Colors + labels a TextView as a soft-fill status pill (see AccentColors.applyPill) - the
 * Results section's Promotion Status badge, reused by Student/HOD/Admin semester-result
 * screens. Returns the color used so callers can also apply it to a card's accent bar. */
public final class PromotionStatusChipHelper {

    private PromotionStatusChipHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, PromotionStatus status) {
        Context context = chip.getContext();
        int colorRes;
        int textRes;
        if (status == PromotionStatus.PROMOTED) {
            colorRes = R.color.status_success;
            textRes = R.string.promotion_status_promoted;
        } else if (status == PromotionStatus.ELIGIBLE_FOR_PROMOTION) {
            colorRes = R.color.status_info;
            textRes = R.string.promotion_status_eligible_for_promotion;
        } else if (status == PromotionStatus.NOT_PROMOTED) {
            colorRes = R.color.error_color;
            textRes = R.string.promotion_status_not_promoted;
        } else {
            colorRes = R.color.on_surface_variant_color;
            textRes = R.string.promotion_status_not_evaluated;
        }
        AccentColors.applyPill(chip, colorRes, context.getString(textRes));
        return colorRes;
    }
}
