package com.example.uos_lms.feature.student.presentation.results;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.example.uos_lms.core.domain.model.SubjectResultSnapshot;
import com.example.uos_lms.core.export.PdfReportGenerator;
import com.example.uos_lms.core.export.ShareFileHelper;
import com.example.uos_lms.core.ui.AcademicStatusHelper;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.ResultStatusBadgeHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** Premium Result Management dashboard: an Overall CGPA hero card, a Performance Summary row,
 * and a 2-column grid of semester tiles (tapping one opens {@link StudentSemesterResultFragment}
 * directly - the old intermediate "select a semester" list screen is gone, its whole job now
 * lives in this grid). All figures come straight from {@link StudentResultsViewModel}'s real
 * backend rollups - nothing here is computed or hard-coded on this screen. */
@AndroidEntryPoint
public class StudentResultsFragment extends Fragment {

    private StudentResultsViewModel viewModel;
    private boolean sectionsAnimated;

    public StudentResultsFragment() {
        super(R.layout.fragment_student_results);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentResultsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.results_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_results_published_yet);
        View progressLoading = view.findViewById(R.id.progressLoading);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        RecyclerView recyclerSemesters = view.findViewById(R.id.recyclerSemesters);
        recyclerSemesters.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerSemesters.setNestedScrollingEnabled(false);
        SimpleListAdapter<StudentSemesterResultSummary> adapter = new SimpleListAdapter<>(
                R.layout.item_student_semester_result_card, this::bindSemesterCard);
        recyclerSemesters.setAdapter(adapter);

        view.findViewById(R.id.buttonDownloadTranscript).setOnClickListener(v -> {
            StudentResultsUiState current = viewModel.getUiState().getValue();
            if (current != null) downloadTranscript(view, current);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            emptyState.setVisibility(!state.isLoading() && !state.isHasResults() ? View.VISIBLE : View.GONE);
            swipeRefresh.setVisibility(!state.isLoading() && state.isHasResults() ? View.VISIBLE : View.GONE);
            refreshBinding.setRefreshing(state.isRefreshing());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeError();
            }

            if (state.isLoading() || !state.isHasResults()) return;

            ((TextView) view.findViewById(R.id.textCgpa)).setText(String.format(Locale.getDefault(), "%.2f", state.getCgpa()));
            AcademicStatusHelper.bind(view.findViewById(R.id.textAcademicStatus), state.getCgpa());
            ((TextView) view.findViewById(R.id.textSemesterCountMini)).setText(String.valueOf(state.getSemesterCount()));
            ((TextView) view.findViewById(R.id.textCreditHoursMini)).setText(String.valueOf(state.getTotalCreditHours()));
            ((TextView) view.findViewById(R.id.textTotalSubjectsMini)).setText(String.valueOf(state.getTotalSubjects()));

            bindStatCard(view.findViewById(R.id.statCgpa), R.drawable.ic_grade, R.string.overall_cgpa_label,
                    String.format(Locale.getDefault(), "%.2f", state.getCgpa()),
                    R.color.role_student_start, R.color.student_container, R.color.on_student_container);
            bindStatCard(view.findViewById(R.id.statBestGpa), R.drawable.ic_trending_up, R.string.best_gpa_label,
                    String.format(Locale.getDefault(), "%.2f", state.getBestGpa()),
                    R.color.status_info, R.color.teacher_container, R.color.on_teacher_container);
            bindStatCard(view.findViewById(R.id.statCompletedSemesters), R.drawable.ic_calendar_month, R.string.completed_semesters_label,
                    String.valueOf(state.getSemesterCount()),
                    R.color.status_warning, R.color.amber_tertiary_container, R.color.on_amber_tertiary_container);
            bindStatCard(view.findViewById(R.id.statTotalCreditHours), R.drawable.ic_school, R.string.total_credit_hours_label,
                    String.valueOf(state.getTotalCreditHours()),
                    R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container);

            adapter.submitList(state.getSortedResults());

            if (!sectionsAnimated) {
                sectionsAnimated = true;
                AnimUtils.fadeSlideIn(view.findViewById(R.id.heroCard), 0L);
                AnimUtils.fadeSlideIn(view.findViewById(R.id.performanceSummaryContainer), 80L);
                AnimUtils.fadeSlideIn(recyclerSemesters, 140L);
            }
        });
    }

    private void bindSemesterCard(View itemView, StudentSemesterResultSummary semester, int position) {
        ((TextView) itemView.findViewById(R.id.textSemesterLabel)).setText(semester.getSemesterLabel());
        ((TextView) itemView.findViewById(R.id.textSessionLabel)).setText(semester.getSessionLabel());

        boolean passed = semester.getFailedSubjectCount() == 0;
        int gpaColorRes = passed ? R.color.status_success : R.color.error_color;
        TextView textGpa = itemView.findViewById(R.id.textGpaValue);
        textGpa.setText(getString(R.string.gpa_semester_format, String.format(Locale.getDefault(), "%.2f", semester.getSemesterGpa())));
        textGpa.setTextColor(ContextCompat.getColor(requireContext(), gpaColorRes));

        ((TextView) itemView.findViewById(R.id.textSubjectsCredits)).setText(getString(
                R.string.subjects_credit_hours_format, semester.getTotalSubjectCount(), semester.getTotalCreditHours()));

        ResultStatusBadgeHelper.bindPassFail(itemView.findViewById(R.id.textPassedFailed), semester);
        ResultStatusBadgeHelper.bindCompletionStatus(itemView.findViewById(R.id.textResultStatus), semester);

        AccentColors.applyBar(itemView.findViewById(R.id.accentBar), gpaColorRes);
        AnimUtils.applyPressScale(itemView);

        itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("semesterId", semester.getSemesterId());
            NavHostFragment.findNavController(this).navigate(R.id.studentSemesterResultFragment, args);
        });
    }

    /** Same premium stat-tile binding used across the Admin/HOD/Teacher dashboards' Overview
     * rows (see item_stat_card.xml) - a String value here since CGPA/GPA are decimals, unlike
     * those screens' integer counts. */
    private void bindStatCard(View card, @DrawableRes int iconRes, @StringRes int labelRes, String value,
            @ColorRes int accentColorRes, @ColorRes int containerColorRes, @ColorRes int onContainerColorRes) {
        AccentColors.applyBar(card.findViewById(R.id.accentBar), accentColorRes);
        card.findViewById(R.id.iconBackground).setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), containerColorRes)));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), onContainerColorRes));
        ((TextView) card.findViewById(R.id.textValue)).setText(value);
        ((TextView) card.findViewById(R.id.textLabel)).setText(labelRes);
    }

    private void downloadTranscript(View view, StudentResultsUiState state) {
        List<String> headers = Arrays.asList("Semester", "Subject", "Code", "Credit Hrs", "Marks", "Grade", "GPA");
        List<List<String>> rows = new ArrayList<>();
        for (StudentSemesterResultSummary semester : state.getResults()) {
            if (semester.getResultStatus() != ResultStatus.APPROVED) continue;
            for (SubjectResultSnapshot subject : semester.getSubjectResults()) {
                rows.add(Arrays.asList(
                        semester.getSemesterLabel(),
                        subject.getSubjectName(),
                        subject.getCourseCode(),
                        String.valueOf(subject.getCreditHours()),
                        String.valueOf((int) Math.round(subject.getMarks())),
                        subject.getGrade(),
                        String.format(Locale.getDefault(), "%.2f", subject.getGpa())));
            }
        }
        String title = getString(R.string.transcript_pdf_title_format, String.format(Locale.getDefault(), "%.2f", state.getCgpa()));
        try {
            byte[] pdf = PdfReportGenerator.generate(title, headers, rows);
            ShareFileHelper.sharePdf(requireContext(), "transcript.pdf", pdf, getString(R.string.download_transcript_pdf));
        } catch (IllegalStateException e) {
            Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }
}
