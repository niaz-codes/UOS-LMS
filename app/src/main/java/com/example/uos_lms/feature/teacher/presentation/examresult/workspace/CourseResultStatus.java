package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import com.example.uos_lms.R;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

/** Presentation-only rollup of a course's result-entry progress, derived purely from the
 * teacher's own {@link com.example.uos_lms.core.domain.model.ExamResult} rows for that subject
 * against its roster size - not a stored field, not a second grading system. */
public enum CourseResultStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, SUBMITTED, APPROVED;

    @StringRes
    public int labelRes() {
        switch (this) {
            case IN_PROGRESS: return R.string.course_status_in_progress;
            case COMPLETED: return R.string.course_status_completed;
            case SUBMITTED: return R.string.course_status_submitted;
            case APPROVED: return R.string.course_status_approved;
            case NOT_STARTED:
            default: return R.string.course_status_not_started;
        }
    }

    @ColorRes
    public int colorRes() {
        switch (this) {
            case IN_PROGRESS: return R.color.status_warning;
            case COMPLETED: return R.color.status_info;
            case SUBMITTED: return R.color.role_hod_start;
            case APPROVED: return R.color.status_success;
            case NOT_STARTED:
            default: return R.color.on_surface_variant_color;
        }
    }
}
