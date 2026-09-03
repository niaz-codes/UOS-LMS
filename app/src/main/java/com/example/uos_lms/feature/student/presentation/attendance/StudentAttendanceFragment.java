package com.example.uos_lms.feature.student.presentation.attendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.example.uos_lms.core.ui.BarChartHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StudentAttendanceFragment extends Fragment {

    private StudentAttendanceViewModel viewModel;

    public StudentAttendanceFragment() {
        super(R.layout.fragment_student_attendance);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentAttendanceViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.attendance_screen_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View chartEmptyState = view.findViewById(R.id.chartEmptyState);
        ((TextView) chartEmptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attendance_recorded);
        LinearLayout chartContainer = view.findViewById(R.id.chartContainer);

        view.findViewById(R.id.buttonManageLeave).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentLeaveFragment));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navAttendance);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navAttendance) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.studentDashboardFragment, false);
            else if (id == R.id.navSubmissions) NavHostFragment.findNavController(this).navigate(R.id.studentSubmissionsFragment);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.studentResultsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
            View contentContainer = view.findViewById(R.id.contentContainer);
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(state.isLoading() ? View.GONE : View.VISIBLE);
            if (state.isLoading()) return;

            ((TextView) view.findViewById(R.id.textOverallPercentage)).setText(
                    getString(R.string.percent_present_overall_format, state.getOverallPercentage()));
            ((TextView) view.findViewById(R.id.textAttendedCount)).setText(
                    getString(R.string.classes_attended_format, state.getPresentCount(), state.getTotalCount()));

            boolean hasData = !state.getBySubject().isEmpty();
            chartEmptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
            chartContainer.setVisibility(hasData ? View.VISIBLE : View.GONE);
            if (hasData) BarChartHelper.render(chartContainer, state.getBySubject(), "%");

            bindLeaveCard(view, state.getApprovedLeaves());

            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindLeaveCard(View view, java.util.List<LeaveApplication> approvedLeaves) {
        View cardLeave = view.findViewById(R.id.cardLeave);
        cardLeave.setVisibility(approvedLeaves.isEmpty() ? View.GONE : View.VISIBLE);
        if (approvedLeaves.isEmpty()) return;

        LinearLayout container = view.findViewById(R.id.leaveRowsContainer);
        container.removeAllViews();
        for (LeaveApplication leave : approvedLeaves) {
            TextView row = new TextView(requireContext());
            row.setTextSize(13);
            row.setText(getString(R.string.leave_date_range_format,
                    DateKeyUtils.millisToDisplay(leave.getFromDateMillis()), DateKeyUtils.millisToDisplay(leave.getToDateMillis())));
            container.addView(row);
        }
    }
}
