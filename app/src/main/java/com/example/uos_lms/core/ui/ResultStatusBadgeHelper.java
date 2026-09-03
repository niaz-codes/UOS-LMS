package com.example.uos_lms.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;

/** Colors + icons the two status lines on a semester result card/tile - "N Passed [/ N Failed]"
 * and the overall PASS/FAIL/INCOMPLETE/PENDING completion line - shared by every screen that
 * shows a {@link StudentSemesterResultSummary} card. A Student's own result feed only ever
 * returns APPROVED rollups (see backend/src/controllers/examResultController.js), so
 * PENDING/INCOMPLETE won't appear there today, but this stays correct if an HOD/Admin view of
 * the same summary type reuses it for their (unfiltered) rollups later. */
public final class ResultStatusBadgeHelper {

    private ResultStatusBadgeHelper() {
    }

    /** "N Passed" (green), or "N Passed  ·  N Failed" (red) once any subject failed. */
    public static void bindPassFail(TextView chip, StudentSemesterResultSummary semester) {
        Context context = chip.getContext();
        boolean passed = semester.getFailedSubjectCount() == 0;
        int colorRes = passed ? R.color.status_success : R.color.error_color;
        String text = context.getString(R.string.x_passed_format, semester.getPassedSubjectCount());
        if (!passed) {
            text += "   " + context.getString(R.string.x_failed_format, semester.getFailedSubjectCount());
        }
        applyLine(chip, text, colorRes, passed ? R.drawable.ic_check_single : R.drawable.ic_error);
    }

    /** "Result Completed" (green/APPROVED), "Incomplete" (amber/REJECTED, needs correction), or
     * "Pending" (blue/DRAFT or awaiting HOD approval). */
    public static void bindCompletionStatus(TextView chip, StudentSemesterResultSummary semester) {
        int colorRes;
        int textRes;
        int iconRes;
        switch (semester.getResultStatus()) {
            case REJECTED:
                colorRes = R.color.status_warning;
                textRes = R.string.result_status_incomplete;
                iconRes = R.drawable.ic_error;
                break;
            case DRAFT:
            case PENDING_HOD_APPROVAL:
                colorRes = R.color.status_info;
                textRes = R.string.result_status_pending;
                iconRes = R.drawable.ic_hourglass_empty;
                break;
            case APPROVED:
            default:
                colorRes = R.color.status_success;
                textRes = R.string.result_completed_label;
                iconRes = R.drawable.ic_check_single;
                break;
        }
        applyLine(chip, chip.getContext().getString(textRes), colorRes, iconRes);
    }

    private static void applyLine(TextView chip, String text, int colorRes, int iconRes) {
        int color = ContextCompat.getColor(chip.getContext(), colorRes);
        chip.setText(text);
        chip.setTextColor(color);
        chip.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(chip, ColorStateList.valueOf(color));
    }
}
