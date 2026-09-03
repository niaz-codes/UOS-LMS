package com.example.uos_lms.feature.hod.presentation.dashboard;

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
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.session.ThemePreferenceManager;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.NotificationBadgeHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.ThemeToggleHelper;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;
import com.example.uos_lms.feature.notifications.AppNotificationCenter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodDashboardFragment extends Fragment {

    @Inject
    AppNotificationCenter notificationCenter;
    @Inject
    ThemePreferenceManager themePreferenceManager;

    private HodDashboardViewModel viewModel;

    public HodDashboardFragment() {
        super(R.layout.fragment_hod_dashboard);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodDashboardViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ThemeToggleHelper.applyIcon(toolbar.getMenu().findItem(R.id.actionThemeToggle), requireContext());
        BadgeDrawable notificationBadge = NotificationBadgeHelper.attach(requireContext(), toolbar, R.id.actionNotifications);
        notificationCenter.getUnreadCount().observe(getViewLifecycleOwner(), count -> NotificationBadgeHelper.update(notificationBadge, count));

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        RefreshUx.Binding refreshBinding = RefreshUx.bindMenuItem(
                toolbar.getMenu().findItem(R.id.actionRefresh), swipeRefresh, () -> viewModel.refresh());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionLogout) {
                viewModel.logout();
                AuthNavigator.navigateToLoginClearingStack(NavHostFragment.findNavController(this));
                return true;
            }
            if (item.getItemId() == R.id.actionNotifications) {
                NavHostFragment.findNavController(this).navigate(R.id.notificationsFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionThemeToggle) {
                ThemeToggleHelper.toggle(requireContext(), themePreferenceManager);
                return true;
            }
            if (item.getItemId() == R.id.actionRefresh) {
                refreshBinding.trigger();
                return true;
            }
            return false;
        });

        View header = view.findViewById(R.id.gradientHeader);
        ((TextView) header.findViewById(R.id.textGreeting)).setText(R.string.welcome_back_admin);
        ((TextView) header.findViewById(R.id.textSubtitle)).setText(R.string.head_of_department);

        bindAction(view, R.id.actionTeachers, R.drawable.ic_group, R.string.action_hod_teachers_title, R.string.action_hod_teachers_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodTeachersFragment));
        bindAction(view, R.id.actionStudents, R.drawable.ic_school, R.string.action_hod_students_title, R.string.action_hod_students_subtitle,
                R.color.student_container, R.color.on_student_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodStudentsFragment));
        bindAction(view, R.id.actionAttendance, R.drawable.ic_event_available, R.string.action_hod_attendance_title, R.string.action_hod_attendance_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodAttendanceManagementFragment));
        bindAction(view, R.id.actionReports, R.drawable.ic_assessment, R.string.action_hod_reports_title, R.string.action_hod_reports_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodReportsFragment));
        bindAction(view, R.id.actionAssignmentMonitor, R.drawable.ic_assignment, R.string.action_hod_assignment_monitor_title, R.string.action_hod_assignment_monitor_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodAssignmentMonitorFragment));
        bindAction(view, R.id.actionQuizMonitor, R.drawable.ic_quiz, R.string.action_hod_quiz_monitor_title, R.string.action_hod_quiz_monitor_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodQuizMonitorFragment));
        bindAction(view, R.id.actionExamResultApprovals, R.drawable.ic_grade, R.string.action_hod_examresult_approvals_title, R.string.action_hod_examresult_approvals_subtitle,
                R.color.student_container, R.color.on_student_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodExamResultApprovalsFragment));
        bindAction(view, R.id.actionResults, R.drawable.ic_grade, R.string.action_results_title, R.string.action_results_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodResultsFragment));
        bindAction(view, R.id.actionRepeatExamReview, R.drawable.ic_replay, R.string.action_hod_repeat_exam_review_title, R.string.action_hod_repeat_exam_review_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodRepeatExamApprovalFragment));
        bindAction(view, R.id.actionLeave, R.drawable.ic_event_busy, R.string.action_leave_title, R.string.action_leave_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodLeaveFragment));
        bindAction(view, R.id.actionTeacherLeave, R.drawable.ic_event_busy, R.string.action_teacher_leave_title, R.string.action_teacher_leave_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodTeacherLeaveFragment));
        bindAction(view, R.id.actionTimetable, R.drawable.ic_calendar_month, R.string.action_timetable_title, R.string.action_timetable_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodTimetableFragment));
        bindAction(view, R.id.actionExamSchedule, R.drawable.ic_calendar_month, R.string.action_examschedule_title, R.string.action_examschedule_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodExamScheduleFragment));
        bindAction(view, R.id.actionMessaging, R.drawable.ic_chat, R.string.action_messaging_title, R.string.action_messaging_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.messagingInboxFragment));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_department_assigned_message);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navHome);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) return true;
            if (id == R.id.navTeachers) NavHostFragment.findNavController(this).navigate(R.id.hodTeachersFragment);
            else if (id == R.id.navStudents) NavHostFragment.findNavController(this).navigate(R.id.hodStudentsFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.hodReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            render(view, header, emptyState, state);
            refreshBinding.setRefreshing(state.isRefreshing());
            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void render(View view, View header, View emptyState, HodDashboardUiState state) {
        ((com.google.android.material.appbar.MaterialToolbar) view.findViewById(R.id.toolbar))
                .setTitle(state.getDepartmentName().isBlank() ? getString(R.string.hod_dashboard_fallback_title) : state.getDepartmentName());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        boolean noDepartment = !state.isLoading() && state.getDepartmentId() == null;

        progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(noDepartment ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(!state.isLoading() && !noDepartment ? View.VISIBLE : View.GONE);
        if (state.isLoading() || noDepartment) return;

        ((TextView) header.findViewById(R.id.textTitle)).setText(state.getFullName());

        bindOverviewCard(view, R.id.statStudents, R.drawable.ic_school, getString(R.string.stat_students),
                R.color.role_student_start, R.color.student_container, R.color.on_student_container,
                String.valueOf(state.getStudentCount()),
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodStudentsFragment));

        bindOverviewCard(view, R.id.statTeachers, R.drawable.ic_group, getString(R.string.stat_teachers),
                R.color.role_teacher_start, R.color.teacher_container, R.color.on_teacher_container,
                String.valueOf(state.getTeacherCount()),
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodTeachersFragment));

        bindOverviewCard(view, R.id.statStudentAttendance, R.drawable.ic_event_available, getString(R.string.student_attendance_title),
                R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container,
                getString(R.string.view_action_label),
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodAttendanceManagementFragment));

        bindOverviewCard(view, R.id.statReports, R.drawable.ic_assessment, getString(R.string.action_hod_reports_title),
                R.color.status_warning, R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                getString(R.string.view_action_label),
                v -> NavHostFragment.findNavController(this).navigate(R.id.hodReportsFragment));

        view.findViewById(R.id.textNoSemesters).setVisibility(state.getSemesters().isEmpty() ? View.VISIBLE : View.GONE);

        LinearLayout semestersContainer = view.findViewById(R.id.semestersContainer);
        semestersContainer.removeAllViews();
        for (Semester semester : state.getSemesters()) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_hod_semester_row, semestersContainer, false);
            ((TextView) row.findViewById(R.id.textLabel)).setText(semester.getDisplayName());
            com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar), R.color.role_hod_start);
            row.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("departmentId", state.getDepartmentId());
                args.putString("semesterId", semester.getId());
                NavHostFragment.findNavController(this).navigate(R.id.hodSemesterSubjectsFragment, args);
            });
            semestersContainer.addView(row);
        }
    }

    /** Premium Overview stat card (Admin panel's card style) - clickable, no trend row since
     * these values aren't month-over-month deltas. */
    private void bindOverviewCard(View root, int includeId, int iconRes, String label,
            int accentColorRes, int containerColorRes, int onContainerColorRes, String value, View.OnClickListener listener) {
        View card = root.findViewById(includeId);
        int accentColor = ContextCompat.getColor(requireContext(), accentColorRes);
        int containerColor = ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainerColor = ContextCompat.getColor(requireContext(), onContainerColorRes);

        com.google.android.material.card.MaterialCardView cardSurface = card.findViewById(R.id.cardSurface);
        cardSurface.setCardBackgroundColor(containerColor);
        cardSurface.setStrokeColor(android.content.res.ColorStateList.valueOf(AccentColors.withAlpha(accentColor, 0.35f)));
        cardSurface.setOnClickListener(listener);
        com.example.uos_lms.core.ui.AnimUtils.applyPressScale(cardSurface);

        card.findViewById(R.id.glowHalo).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        card.findViewById(R.id.decorShapeLarge).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainerColor));
        card.findViewById(R.id.decorShapeSmall).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainerColor));

        card.findViewById(R.id.iconBackground).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

        TextView textLabel = card.findViewById(R.id.textLabel);
        textLabel.setText(label);
        textLabel.setTextColor(onContainerColor);

        TextView textValue = card.findViewById(R.id.textValue);
        textValue.setText(value);
        textValue.setTextColor(onContainerColor);

        View trendRow = (View) card.findViewById(R.id.textTrend).getParent();
        trendRow.setVisibility(View.GONE);
    }

    private void bindAction(View root, int includeId, int iconRes, int titleRes, int subtitleRes,
                             int containerColorRes, int onContainerColorRes, View.OnClickListener listener) {
        View card = root.findViewById(includeId);
        card.findViewById(R.id.iconBackground).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), containerColorRes)));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), onContainerColorRes));
        ((TextView) card.findViewById(R.id.textTitle)).setText(titleRes);
        ((TextView) card.findViewById(R.id.textSubtitle)).setText(subtitleRes);
        card.setOnClickListener(listener);
    }
}
