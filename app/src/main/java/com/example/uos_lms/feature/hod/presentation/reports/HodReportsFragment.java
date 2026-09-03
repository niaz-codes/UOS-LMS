package com.example.uos_lms.feature.hod.presentation.reports;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.ui.BarChartHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodReportsFragment extends Fragment {

    private HodReportsViewModel viewModel;

    public HodReportsFragment() {
        super(R.layout.fragment_hod_reports);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodReportsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.department_reports_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        SwipeRefreshLayout contentContainer = view.findViewById(R.id.contentContainer);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), contentContainer, () -> viewModel.refresh());
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
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.hodDashboardFragment, false);
            else if (id == R.id.navTeachers) NavHostFragment.findNavController(this).navigate(R.id.hodTeachersFragment);
            else if (id == R.id.navStudents) NavHostFragment.findNavController(this).navigate(R.id.hodStudentsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(state.isLoading() ? View.GONE : View.VISIBLE);
            if (state.isLoading()) return;

            statRow.removeAllViews();
            addStat(statRow, R.drawable.ic_group, getString(R.string.stat_teachers), state.getTotalTeachers(),
                    R.color.indigo_primary_container, R.color.on_indigo_primary_container);
            addStat(statRow, R.drawable.ic_school, getString(R.string.stat_students), state.getTotalStudents(),
                    R.color.emerald_secondary_container, R.color.on_emerald_secondary_container);
            addStat(statRow, R.drawable.ic_school, getString(R.string.stat_subjects), state.getTotalSubjects(),
                    R.color.amber_tertiary_container, R.color.on_amber_tertiary_container);
            addStat(statRow, R.drawable.ic_calendar_month, getString(R.string.stat_semesters_count), state.getTotalSemesters(),
                    R.color.error_container, R.color.on_error_container);

            boolean hasAttendance = !state.getSemesterAttendance().isEmpty();
            attendanceEmptyState.setVisibility(hasAttendance ? View.GONE : View.VISIBLE);
            attendanceChartContainer.setVisibility(hasAttendance ? View.VISIBLE : View.GONE);
            if (hasAttendance) BarChartHelper.render(attendanceChartContainer, state.getSemesterAttendance(), "%");

            boolean hasTeacherLoad = !state.getTeacherLoad().isEmpty();
            teacherLoadEmptyState.setVisibility(hasTeacherLoad ? View.GONE : View.VISIBLE);
            teacherLoadChartContainer.setVisibility(hasTeacherLoad ? View.VISIBLE : View.GONE);
            if (hasTeacherLoad) BarChartHelper.render(teacherLoadChartContainer, state.getTeacherLoad(), "");

            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
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
