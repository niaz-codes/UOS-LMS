package com.example.uos_lms.core.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.example.uos_lms.core.domain.model.SubjectResultStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders one subject-result row (item_student_exam_result_row.xml's grade-badge card) into
 * a container - shared by every Results screen that shows a subject×grade table (Student's own
 * View Result, HOD/Admin's per-student drill-down), so the row layout/binding logic exists in
 * exactly one place. */
public final class SubjectResultRowBinder {

    private SubjectResultRowBinder() {
    }

    public static void bindRows(LinearLayout container, List<SubjectResultSnapshot> subjects, Map<String, RepeatStatus> repeatStatusBySubjectId) {
        Context context = container.getContext();
        container.removeAllViews();
        long delay = 0L;
        for (SubjectResultSnapshot subject : subjects) {
            View row = buildRow(container, context, subject, repeatStatusBySubjectId.get(subject.getSubjectId()));
            container.addView(row);
            AnimUtils.fadeSlideIn(row, delay);
            delay += 40L;
        }
    }

    private static View buildRow(LinearLayout parent, Context context, SubjectResultSnapshot subject, RepeatStatus repeatStatus) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_student_exam_result_row, parent, false);

        ((TextView) row.findViewById(R.id.textSubjectTitle)).setText(subject.getSubjectName());
        String meta = subject.getCourseCode() + " • " + subject.getCreditHours() + " credit hours";
        if (repeatStatus != null) {
            meta += " • " + context.getString(R.string.repeat_exam_badge_format, repeatStatus.name());
        }
        ((TextView) row.findViewById(R.id.textSubjectMeta)).setText(meta);
        ((TextView) row.findViewById(R.id.textMarks)).setText(
                context.getString(R.string.marks_slash_format, (int) Math.round(subject.getMarks()), 100));
        ((TextView) row.findViewById(R.id.textGpaPoint)).setText(
                context.getString(R.string.gpa_result_format, String.format(Locale.getDefault(), "%.2f", subject.getGpa())));

        int gradeColorRes = AccentColors.colorForGrade(subject.getGrade());
        ((TextView) row.findViewById(R.id.textGrade)).setText(subject.getGrade() != null ? subject.getGrade() : "");
        ((TextView) row.findViewById(R.id.textGrade)).setTextColor(ContextCompat.getColor(context, gradeColorRes));
        row.findViewById(R.id.gradeBackground).setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                AccentColors.withAlpha(ContextCompat.getColor(context, gradeColorRes), 0.18f)));

        int statusColorRes = SubjectResultStatusChipHelper.bind(row.findViewById(R.id.textStatus), subject.getStatus());
        AccentColors.applyBar(row.findViewById(R.id.accentBar),
                subject.getStatus() == SubjectResultStatus.FAIL ? statusColorRes : gradeColorRes);

        return row;
    }
}
