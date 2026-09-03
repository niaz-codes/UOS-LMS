package com.example.uos_lms.feature.teacher.presentation.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.ui.BarChartHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherReportsFragment extends Fragment {

    private TeacherReportsViewModel viewModel;

    public TeacherReportsFragment() {
        super(R.layout.fragment_teacher_reports);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherReportsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.teacher_reports_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        LinearLayout statRow = view.findViewById(R.id.statRow);
        View attendanceEmptyState = view.findViewById(R.id.attendanceEmptyState);
        ((TextView) attendanceEmptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_data);
        LinearLayout attendanceChartContainer = view.findViewById(R.id.attendanceChartContainer);
        View performanceEmptyState = view.findViewById(R.id.performanceEmptyState);
        ((TextView) performanceEmptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_graded_work_yet);
        LinearLayout performanceChartContainer = view.findViewById(R.id.performanceChartContainer);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navExamResult);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navExamResult) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.teacherDashboardFragment, false);
            else if (id == R.id.navStudents) NavHostFragment.findNavController(this).navigate(R.id.teacherStudentsFragment);
            else if (id == R.id.navAttendance) NavHostFragment.findNavController(this).navigate(R.id.teacherAttendanceReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(state.isLoading() ? View.GONE : View.VISIBLE);
            if (state.isLoading()) return;

            statRow.removeAllViews();
            addStat(statRow, R.drawable.ic_school, getString(R.string.stat_subjects), state.getTotalSubjects(),
                    R.color.indigo_primary_container, R.color.on_indigo_primary_container);
            addStat(statRow, R.drawable.ic_group, getString(R.string.stat_students), state.getTotalStudents(),
                    R.color.emerald_secondary_container, R.color.on_emerald_secondary_container);
            addStat(statRow, R.drawable.ic_assignment, getString(R.string.stat_assignments), state.getTotalAssignments(),
                    R.color.amber_tertiary_container, R.color.on_amber_tertiary_container);
            addStat(statRow, R.drawable.ic_quiz, getString(R.string.stat_quizzes), state.getTotalQuizzes(),
                    R.color.error_container, R.color.on_error_container);

            boolean hasAttendance = !state.getAttendanceBySubject().isEmpty();
            attendanceEmptyState.setVisibility(hasAttendance ? View.GONE : View.VISIBLE);
            attendanceChartContainer.setVisibility(hasAttendance ? View.VISIBLE : View.GONE);
            if (hasAttendance) BarChartHelper.render(attendanceChartContainer, state.getAttendanceBySubject(), "%");

            boolean hasPerformance = !state.getPerformanceBySubject().isEmpty();
            performanceEmptyState.setVisibility(hasPerformance ? View.GONE : View.VISIBLE);
            performanceChartContainer.setVisibility(hasPerformance ? View.VISIBLE : View.GONE);
            if (hasPerformance) BarChartHelper.render(performanceChartContainer, state.getPerformanceBySubject(), "%");
        });
    }

    private void addStat(LinearLayout container, int iconRes, String label, int value, int bgColorRes, int contentColorRes) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_stat_card, container, false);
        ((MaterialCardView) card).setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColorRes));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), contentColorRes));
        TextView valueView = card.findViewById(R.id.textValue);
        valueView.setText(String.valueOf(value));
        valueView.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));
        TextView labelView = card.findViewById(R.id.textLabel);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(12);
        card.setLayoutParams(params);
        container.addView(card);
    }
}
