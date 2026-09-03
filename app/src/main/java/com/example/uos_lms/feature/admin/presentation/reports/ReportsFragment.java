package com.example.uos_lms.feature.admin.presentation.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
public class ReportsFragment extends Fragment {

    private ReportsViewModel viewModel;

    public ReportsFragment() {
        super(R.layout.fragment_admin_reports);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.reports_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        LinearLayout statRow = view.findViewById(R.id.statRow);
        View attendanceEmptyState = view.findViewById(R.id.attendanceEmptyState);
        ((TextView) attendanceEmptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_data);
        LinearLayout attendanceChartContainer = view.findViewById(R.id.attendanceChartContainer);
        View teacherLoadEmptyState = view.findViewById(R.id.teacherLoadEmptyState);
        ((TextView) teacherLoadEmptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_teacher_assignments);
        LinearLayout teacherLoadChartContainer = view.findViewById(R.id.teacherLoadChartContainer);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navReports);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navReports) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.adminHomeFragment, false);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.adminResultsDepartmentListFragment);
            else if (id == R.id.navDepartments) NavHostFragment.findNavController(this).navigate(R.id.departmentListFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            statRow.removeAllViews();
            addStat(statRow, R.drawable.ic_school, getString(R.string.stat_students), state.getTotalStudents(),
                    R.color.indigo_primary_container, R.color.on_indigo_primary_container);
            addStat(statRow, R.drawable.ic_group, getString(R.string.stat_teachers), state.getTotalTeachers(),
                    R.color.emerald_secondary_container, R.color.on_emerald_secondary_container);
            addStat(statRow, R.drawable.ic_group, getString(R.string.stat_hods), state.getTotalHods(),
                    R.color.amber_tertiary_container, R.color.on_amber_tertiary_container);
            addStat(statRow, R.drawable.ic_apartment, getString(R.string.stat_departments), state.getTotalDepartments(),
                    R.color.indigo_primary_container, R.color.on_indigo_primary_container);
            addStat(statRow, R.drawable.ic_school, getString(R.string.stat_subjects), state.getTotalSubjects(),
                    R.color.emerald_secondary_container, R.color.on_emerald_secondary_container);
            addStat(statRow, R.drawable.ic_assignment, getString(R.string.stat_assignments), state.getTotalAssignments(),
                    R.color.amber_tertiary_container, R.color.on_amber_tertiary_container);
            addStat(statRow, R.drawable.ic_quiz, getString(R.string.stat_quizzes_exams), state.getTotalQuizzes(),
                    R.color.error_container, R.color.on_error_container);

            boolean hasAttendance = !state.getDepartmentAttendance().isEmpty();
            attendanceEmptyState.setVisibility(hasAttendance ? View.GONE : View.VISIBLE);
            attendanceChartContainer.setVisibility(hasAttendance ? View.VISIBLE : View.GONE);
            if (hasAttendance) {
                BarChartHelper.render(attendanceChartContainer, state.getDepartmentAttendance(), "%");
            }

            boolean hasTeacherLoad = !state.getTeacherLoad().isEmpty();
            teacherLoadEmptyState.setVisibility(hasTeacherLoad ? View.GONE : View.VISIBLE);
            teacherLoadChartContainer.setVisibility(hasTeacherLoad ? View.VISIBLE : View.GONE);
            if (hasTeacherLoad) {
                BarChartHelper.render(teacherLoadChartContainer, state.getTeacherLoad(), "");
            }
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
