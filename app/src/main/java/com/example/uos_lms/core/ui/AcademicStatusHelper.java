package com.example.uos_lms.core.ui;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.example.uos_lms.R;

/** Colors + labels a TextView as a soft-fill "academic standing" pill (see
 * AccentColors.applyPill), derived purely from CGPA against standard HEC-style bands - a
 * presentation-only tier, not a grading rule (the CGPA itself is never recomputed here). */
public final class AcademicStatusHelper {

    private AcademicStatusHelper() {
    }

    @ColorRes
    public static int bind(TextView chip, double cgpa) {
        Context context = chip.getContext();
        int colorRes;
        int textRes;
        if (cgpa >= 3.5) {
            colorRes = R.color.status_success;
            textRes = R.string.academic_status_excellent;
        } else if (cgpa >= 3.0) {
            colorRes = R.color.status_info;
            textRes = R.string.academic_status_very_good;
        } else if (cgpa >= 2.5) {
            colorRes = R.color.role_student_start;
            textRes = R.string.academic_status_good;
        } else if (cgpa >= 2.0) {
            colorRes = R.color.status_warning;
            textRes = R.string.academic_status_satisfactory;
        } else {
            colorRes = R.color.error_color;
            textRes = R.string.academic_status_needs_improvement;
        }
        AccentColors.applyPill(chip, colorRes, context.getString(textRes));
        return colorRes;
    }
}
