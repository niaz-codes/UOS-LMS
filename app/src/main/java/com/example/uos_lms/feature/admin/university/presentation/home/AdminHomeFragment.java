package com.example.uos_lms.feature.admin.university.presentation.home;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateUtils;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.session.ThemePreferenceManager;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.AnimUtils;
import com.example.uos_lms.core.ui.NotificationBadgeHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.example.uos_lms.core.ui.ThemeToggleHelper;
import com.example.uos_lms.core.ui.TrendSparklineView;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;
import com.example.uos_lms.feature.notifications.AppNotificationCenter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminHomeFragment extends Fragment {

    @Inject
    AppNotificationCenter notificationCenter;
    @Inject
    ThemePreferenceManager themePreferenceManager;

    private AdminHomeViewModel viewModel;
    private boolean sectionsAnimated;
    private RefreshUx.Binding refreshBinding;

    public AdminHomeFragment() {
        super(R.layout.fragment_admin_home);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminHomeViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ThemeToggleHelper.applyIcon(toolbar.getMenu().findItem(R.id.actionThemeToggle), requireContext());
        BadgeDrawable notificationBadge = NotificationBadgeHelper.attach(requireContext(), toolbar, R.id.actionNotifications);
        notificationCenter.getUnreadCount().observe(getViewLifecycleOwner(), count -> NotificationBadgeHelper.update(notificationBadge, count));

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshBinding = RefreshUx.bindMenuItem(toolbar.getMenu().findItem(R.id.actionRefresh), swipeRefresh, () -> viewModel.refresh());

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
        ((TextView) header.findViewById(R.id.textTitle)).setText(R.string.administrator);
        ((TextView) header.findViewById(R.id.textSubtitle)).setText(R.string.admin_home_subtitle);

        view.findViewById(R.id.textPendingViewAll).setOnClickListener(v -> openUsersTab(0, "PENDING"));

        bindAction(view, R.id.actionDepartments, R.drawable.ic_apartment, R.string.action_departments_title, R.string.action_departments_subtitle,
                R.color.admin_container, R.color.on_admin_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.departmentListFragment));
        bindAction(view, R.id.actionReports, R.drawable.ic_assessment, R.string.action_reports_title, R.string.action_reports_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminReportsFragment));
        bindAction(view, R.id.actionAttendanceReports, R.drawable.ic_event_available, R.string.action_attendance_reports_title, R.string.action_attendance_reports_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminAttendanceReportsFragment));
        bindAction(view, R.id.actionTeacherAttendance, R.drawable.ic_group, R.string.action_teacher_attendance_title, R.string.action_teacher_attendance_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminTeacherAttendanceFragment));
        bindAction(view, R.id.actionAssignmentMonitor, R.drawable.ic_assignment, R.string.action_assignment_monitor_title, R.string.action_assignment_monitor_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminAssignmentMonitorFragment));
        bindAction(view, R.id.actionQuizMonitor, R.drawable.ic_quiz, R.string.action_quiz_monitor_title, R.string.action_quiz_monitor_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminQuizMonitorFragment));
        bindAction(view, R.id.actionExamResultMonitor, R.drawable.ic_grade, R.string.action_examresult_monitor_title, R.string.action_examresult_monitor_subtitle,
                R.color.student_container, R.color.on_student_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminExamResultMonitorFragment));
        bindAction(view, R.id.actionResults, R.drawable.ic_grade, R.string.action_results_title, R.string.action_results_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminResultsDepartmentListFragment));
        bindAction(view, R.id.actionPromotion, R.drawable.ic_trending_up, R.string.action_promotion_title, R.string.action_promotion_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminPromotionFragment));
        bindAction(view, R.id.actionCalendar, R.drawable.ic_calendar_month, R.string.action_calendar_title, R.string.action_calendar_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.academicCalendarFragment));
        bindAction(view, R.id.actionAnnouncements, R.drawable.ic_campaign, R.string.action_announcements_title, R.string.action_announcements_subtitle,
                R.color.admin_container, R.color.on_admin_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.announcementsFragment));
        bindAction(view, R.id.actionLeave, R.drawable.ic_event_busy, R.string.action_leave_title, R.string.action_leave_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminLeaveFragment));
        bindAction(view, R.id.actionTeacherLeave, R.drawable.ic_event_busy, R.string.action_teacher_leave_title, R.string.action_admin_teacher_leave_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminTeacherLeaveFragment));
        bindAction(view, R.id.actionTimetable, R.drawable.ic_calendar_month, R.string.action_timetable_title, R.string.action_timetable_subtitle,
                R.color.teacher_container, R.color.on_teacher_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminTimetableFragment));
        bindAction(view, R.id.actionExamSchedule, R.drawable.ic_calendar_month, R.string.action_examschedule_title, R.string.action_examschedule_subtitle,
                R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminExamScheduleFragment));
        bindAction(view, R.id.actionMessaging, R.drawable.ic_chat, R.string.action_messaging_title, R.string.action_messaging_subtitle,
                R.color.hod_container, R.color.on_hod_container,
                v -> NavHostFragment.findNavController(this).navigate(R.id.messagingInboxFragment));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navHome);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) return true;
            if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.adminResultsDepartmentListFragment);
            else if (id == R.id.navDepartments) NavHostFragment.findNavController(this).navigate(R.id.departmentListFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.adminReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    @Override
    public void onDestroyView() {
        LinearLayout container = requireView().findViewById(R.id.pendingApprovalsContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View dot = container.getChildAt(i).findViewById(R.id.pillDot);
            if (dot != null) AnimUtils.stopPulse(dot);
        }
        super.onDestroyView();
    }

    private void render(View view, AdminHomeUiState state) {
        bindOverviewCard(view, R.id.statTotalStudents, R.drawable.ic_group, getString(R.string.overview_total_students),
                R.color.role_teacher_start, R.color.teacher_container, R.color.on_teacher_container,
                state.getTotalStudents(), state.getStudentsDelta(),
                v -> NavHostFragment.findNavController(this).navigate(R.id.adminStudentTreeFragment));
        bindOverviewCard(view, R.id.statTotalTeachers, R.drawable.ic_school, getString(R.string.overview_total_teachers),
                R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container,
                state.getTotalTeachers(), state.getTeachersDelta(),
                v -> openUsersTab(2, null));
        bindOverviewCard(view, R.id.statTotalHods, R.drawable.ic_verified_user, getString(R.string.overview_total_hods),
                R.color.role_student_start, R.color.student_container, R.color.on_student_container,
                state.getTotalHods(), state.getHodsDelta(),
                v -> openUsersTab(1, null));
        bindOverviewCard(view, R.id.statTotalDepartments, R.drawable.ic_apartment, getString(R.string.overview_total_departments),
                R.color.status_warning, R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                state.getTotalDepartments(), state.getDepartmentsDelta(),
                v -> NavHostFragment.findNavController(this).navigate(R.id.departmentListFragment));

        renderStudentsSection(view, state);
        renderTeachersSection(view, state);
        renderHodsSection(view, state);
        renderPendingApprovals(view, state);

        refreshBinding.setRefreshing(state.isRefreshing());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessages();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeMessages();
        }

        if (!state.isLoading() && !sectionsAnimated) {
            sectionsAnimated = true;
            AnimUtils.fadeSlideIn(view.findViewById(R.id.overviewGrid), 0L);
            AnimUtils.fadeSlideIn(view.findViewById(R.id.sectionStudents), 60L);
            AnimUtils.fadeSlideIn(view.findViewById(R.id.sectionTeachers), 100L);
            AnimUtils.fadeSlideIn(view.findViewById(R.id.sectionHods), 140L);
        }
    }

    private void bindOverviewCard(View root, int includeId, @DrawableRes int iconRes, String label,
            @ColorRes int accentColorRes, @ColorRes int containerColorRes, @ColorRes int onContainerColorRes,
            int value, int delta, View.OnClickListener listener) {
        View card = root.findViewById(includeId);
        int accentColor = ContextCompat.getColor(requireContext(), accentColorRes);
        int containerColor = ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainerColor = ContextCompat.getColor(requireContext(), onContainerColorRes);

        MaterialCardView cardSurface = card.findViewById(R.id.cardSurface);
        cardSurface.setCardBackgroundColor(containerColor);
        cardSurface.setStrokeColor(ColorStateList.valueOf(AccentColors.withAlpha(accentColor, 0.35f)));
        cardSurface.setOnClickListener(listener);
        AnimUtils.applyPressScale(cardSurface);

        card.findViewById(R.id.glowHalo).setBackgroundTintList(ColorStateList.valueOf(accentColor));
        card.findViewById(R.id.decorShapeLarge).setBackgroundTintList(ColorStateList.valueOf(onContainerColor));
        card.findViewById(R.id.decorShapeSmall).setBackgroundTintList(ColorStateList.valueOf(onContainerColor));

        card.findViewById(R.id.iconBackground).setBackgroundTintList(ColorStateList.valueOf(accentColor));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

        TextView textLabel = card.findViewById(R.id.textLabel);
        textLabel.setText(label);
        textLabel.setTextColor(onContainerColor);

        TextView textValue = card.findViewById(R.id.textValue);
        textValue.setTextColor(onContainerColor);
        AnimUtils.animateCount(textValue, value);

        TextView textTrend = card.findViewById(R.id.textTrend);
        TrendSparklineView sparkline = card.findViewById(R.id.sparkline);
        sparkline.setAccentColor(accentColor);
        boolean up = delta > 0;
        sparkline.setTrendUp(up);
        if (up) {
            int success = ContextCompat.getColor(requireContext(), R.color.status_success);
            textTrend.setText(getString(R.string.overview_trend_up_format, delta));
            textTrend.setTextColor(success);
            Drawable trendIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trending_up);
            if (trendIcon != null) {
                int iconSizePx = Math.round(12 * getResources().getDisplayMetrics().density);
                trendIcon.setBounds(0, 0, iconSizePx, iconSizePx);
            }
            textTrend.setCompoundDrawables(trendIcon, null, null, null);
            TextViewCompat.setCompoundDrawableTintList(textTrend, ColorStateList.valueOf(success));
        } else {
            textTrend.setText(R.string.overview_trend_no_change);
            textTrend.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant_color));
            textTrend.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    // ---- Students / Teachers / HODs: premium expandable role cards (icon badge + big count,
    // matching the Overview cards' visual language). Each one is collapsed to a one-line summary
    // until tapped; the chip rows and result rows are rebuilt from scratch on every render
    // (cheap - these lists are short) so there's no separate "diff" logic to keep in sync with
    // the ViewModel state. Students stays a network-driven Department -> Session -> Semester
    // drill-down (one row per level, stacked); Teachers/HODs show every user of that role
    // immediately (already in the allUsers cache - no network round trip) with an optional
    // Department filter row, per the "show ALL first, then filter" spec. ----

    private void renderStudentsSection(View view, AdminHomeUiState state) {
        View section = view.findViewById(R.id.sectionStudents);
        boolean expanded = state.isStudentsSectionExpanded();
        bindSectionHeader(section, R.drawable.ic_group, R.color.role_teacher_start, state.getTotalStudents(),
                getString(R.string.drilldown_students_title), getString(R.string.drilldown_students_summary),
                expanded, () -> viewModel.toggleStudentsSection());

        View expandedContent = section.findViewById(R.id.expandedContent);
        expandedContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!expanded) return;

        LinearLayout chipRowsContainer = section.findViewById(R.id.chipRowsContainer);
        chipRowsContainer.removeAllViews();

        LinearLayout departmentRow = addChipLevelRow(chipRowsContainer, true);
        buildDepartmentChips(departmentRow, state.getAllDepartments(), state.getStudentsDepartmentId(),
                R.color.role_teacher_start, viewModel::selectStudentsDepartment);

        String departmentId = state.getStudentsDepartmentId();
        if (departmentId != null) {
            LinearLayout sessionRow = addChipLevelRow(chipRowsContainer, false);
            if (state.isStudentsSessionsLoading()) {
                addInfoChip(sessionRow, getString(R.string.drilldown_loading));
            } else if (state.getStudentsSessions().isEmpty()) {
                addInfoChip(sessionRow, getString(R.string.drilldown_no_sessions));
            } else {
                for (Session session : state.getStudentsSessions()) {
                    sessionRow.addView(buildChip(sessionRow, session.getLabel(),
                            session.getId().equals(state.getStudentsSessionId()), R.color.role_teacher_start,
                            v -> viewModel.selectStudentsSession(session.getId())));
                }
            }
        }

        if (state.getStudentsSessionId() != null) {
            LinearLayout semesterRow = addChipLevelRow(chipRowsContainer, false);
            if (state.isStudentsSemestersLoading()) {
                addInfoChip(semesterRow, getString(R.string.drilldown_loading));
            } else if (state.getStudentsSemesters().isEmpty()) {
                addInfoChip(semesterRow, getString(R.string.drilldown_no_semesters));
            } else {
                for (Semester semester : state.getStudentsSemesters()) {
                    semesterRow.addView(buildChip(semesterRow, semester.getDisplayName(),
                            semester.getId().equals(state.getStudentsSemesterId()), R.color.role_teacher_start,
                            v -> viewModel.selectStudentsSemester(semester.getId())));
                }
            }
        }

        boolean pathComplete = state.getStudentsSemesterId() != null;
        renderResultList(section, pathComplete, state.isStudentsRosterLoading(), state.getStudentsRoster(),
                getString(R.string.drilldown_students_prompt), getString(R.string.no_students_found_scope),
                (parent, student) -> buildStudentResultRow(parent, student, state));
    }

    private void renderTeachersSection(View view, AdminHomeUiState state) {
        View section = view.findViewById(R.id.sectionTeachers);
        boolean expanded = state.isTeachersSectionExpanded();
        bindSectionHeader(section, R.drawable.ic_school, R.color.role_hod_start, state.getTotalTeachers(),
                getString(R.string.drilldown_teachers_title), getString(R.string.drilldown_teachers_summary),
                expanded, () -> viewModel.toggleTeachersSection());

        View expandedContent = section.findViewById(R.id.expandedContent);
        expandedContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!expanded) return;

        LinearLayout chipRowsContainer = section.findViewById(R.id.chipRowsContainer);
        chipRowsContainer.removeAllViews();
        LinearLayout departmentRow = addChipLevelRow(chipRowsContainer, true);
        buildDepartmentFilterChips(departmentRow, state.getAllDepartments(), state.getTeachersDepartmentFilterId(),
                R.color.role_hod_start, viewModel::selectTeachersDepartmentFilter);

        boolean stillLoadingUsers = state.isLoading() && state.getTeachersRoster().isEmpty();
        renderResultList(section, true, stillLoadingUsers, state.getTeachersRoster(),
                "", getString(R.string.drilldown_no_teachers_found),
                (parent, teacher) -> buildTeacherResultRow(parent, teacher, state));
    }

    private void renderHodsSection(View view, AdminHomeUiState state) {
        View section = view.findViewById(R.id.sectionHods);
        boolean expanded = state.isHodsSectionExpanded();
        bindSectionHeader(section, R.drawable.ic_verified_user, R.color.role_student_start, state.getTotalHods(),
                getString(R.string.drilldown_hods_title), getString(R.string.drilldown_hods_summary),
                expanded, () -> viewModel.toggleHodsSection());

        View expandedContent = section.findViewById(R.id.expandedContent);
        expandedContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!expanded) return;

        LinearLayout chipRowsContainer = section.findViewById(R.id.chipRowsContainer);
        chipRowsContainer.removeAllViews();
        LinearLayout departmentRow = addChipLevelRow(chipRowsContainer, true);
        buildDepartmentFilterChips(departmentRow, state.getAllDepartments(), state.getHodsDepartmentFilterId(),
                R.color.role_student_start, viewModel::selectHodsDepartmentFilter);

        boolean stillLoadingUsers = state.isLoading() && state.getHodsRoster().isEmpty();
        renderResultList(section, true, stillLoadingUsers, state.getHodsRoster(),
                "", getString(R.string.drilldown_no_hod_found),
                (parent, hod) -> buildHodResultRow(parent, hod, state));
    }

    /** Vivid icon badge + a large count, matching the Overview cards' visual language - the
     * premium mini-card look, just laid out full-width instead of compact-and-centered. */
    private void bindSectionHeader(View section, @DrawableRes int iconRes, @ColorRes int accentColorRes, int value,
            String title, String summary, boolean expanded, Runnable onToggle) {
        int accentColor = ContextCompat.getColor(requireContext(), accentColorRes);
        section.findViewById(R.id.iconBackground).setBackgroundTintList(ColorStateList.valueOf(accentColor));
        ImageView icon = section.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));

        AnimUtils.animateCount(section.findViewById(R.id.textValue), value);
        TextView textTitle = section.findViewById(R.id.textTitle);
        textTitle.setText(title);
        textTitle.setTextColor(accentColor);
        ((TextView) section.findViewById(R.id.textSummary)).setText(summary);

        ImageView chevron = section.findViewById(R.id.imageChevron);
        float targetRotation = expanded ? 180f : 0f;
        if (chevron.getRotation() != targetRotation) {
            chevron.animate().rotation(targetRotation).setDuration(180L).start();
        }
        section.findViewById(R.id.headerRow).setOnClickListener(v -> onToggle.run());
    }

    /** Renders the roster/result area shared by all three sections: a prompt while the path
     * isn't complete yet (Students only - Teachers/HODs are always "complete", they show
     * everyone by default), a spinner while loading, an empty message, or the rows themselves. */
    private <T> void renderResultList(View section, boolean pathComplete, boolean loading, List<T> items,
            String promptMessage, String emptyMessage, RowBinder<T> rowBinder) {
        View progress = section.findViewById(R.id.progressResult);
        TextView emptyText = section.findViewById(R.id.textEmptyResult);
        LinearLayout resultContainer = section.findViewById(R.id.resultContainer);
        resultContainer.removeAllViews();

        if (!pathComplete) {
            progress.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(promptMessage);
            return;
        }
        if (loading) {
            progress.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.GONE);
        if (items.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(emptyMessage);
            return;
        }
        emptyText.setVisibility(View.GONE);
        for (T item : items) {
            resultContainer.addView(rowBinder.bind(resultContainer, item));
        }
    }

    private interface RowBinder<T> {
        View bind(LinearLayout parent, T item);
    }

    /** Students: name, photo, roll/registration numbers, the exact dept·session·semester scope
     * just picked, email, and status - reusing item_student_card.xml, the same row the dedicated
     * Admin Student Management screens already use. */
    private View buildStudentResultRow(LinearLayout parent, User student, AdminHomeUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_card, parent, false);
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), student.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(student.getFullName());
        ((TextView) row.findViewById(R.id.textRegistrationNumber)).setText(
                getString(R.string.registration_number) + ": " + orNotAssigned(student.getRegistrationNumber()));
        ((TextView) row.findViewById(R.id.textRollNumber)).setText(
                getString(R.string.roll_number) + ": " + orNotAssigned(student.getRollNumber()));

        String deptName = findDepartmentName(state.getAllDepartments(), state.getStudentsDepartmentId());
        String sessionLabel = findSessionLabel(state.getStudentsSessions(), state.getStudentsSessionId());
        String semesterName = findSemesterName(state.getStudentsSemesters(), state.getStudentsSemesterId());
        ((TextView) row.findViewById(R.id.textScope)).setText(deptName + " • " + sessionLabel + " • " + semesterName);
        ((TextView) row.findViewById(R.id.textEmail)).setText(student.getEmail());
        AccentColors.applyBar(row.findViewById(R.id.accentBar),
                StatusChipHelper.bind(row.findViewById(R.id.textStatusChip), student.getStatus()));

        row.findViewById(R.id.buttonMore).setVisibility(View.GONE);
        row.findViewById(R.id.clickableArea).setOnClickListener(v -> openUserDetail(student.getUid()));
        return row;
    }

    /** Teachers: name, photo, email, designation, every department they're assigned to, status. */
    private View buildTeacherResultRow(LinearLayout parent, User teacher, AdminHomeUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_user_row, parent, false);
        AccentColors.applyBar(row.findViewById(R.id.accentBar), R.color.role_hod_start);
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), teacher.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(teacher.getFullName());
        ((TextView) row.findViewById(R.id.textEmail)).setText(teacher.getEmail());

        bindOptionalLine(row.findViewById(R.id.textExtra1), teacher.getDesignation());
        bindOptionalLine(row.findViewById(R.id.textExtra2), joinDepartmentNames(state.getAllDepartments(), teacher.getDepartmentIds()));

        StatusChipHelper.bind(row.findViewById(R.id.textStatusChip), teacher.getStatus());
        row.findViewById(R.id.buttonMore).setVisibility(View.GONE);
        row.findViewById(R.id.clickableArea).setOnClickListener(v -> openUserDetail(teacher.getUid()));
        return row;
    }

    /** HODs: name, photo, email, and - since an HOD belongs to exactly one department - that
     * department shown prominently, plus status. */
    private View buildHodResultRow(LinearLayout parent, User hod, AdminHomeUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_user_row, parent, false);
        AccentColors.applyBar(row.findViewById(R.id.accentBar), R.color.role_student_start);
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), hod.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(hod.getFullName());
        ((TextView) row.findViewById(R.id.textEmail)).setText(hod.getEmail());

        bindOptionalLine(row.findViewById(R.id.textExtra1), findDepartmentName(state.getAllDepartments(), hod.getDepartment()));

        StatusChipHelper.bind(row.findViewById(R.id.textStatusChip), hod.getStatus());
        row.findViewById(R.id.buttonMore).setVisibility(View.GONE);
        row.findViewById(R.id.clickableArea).setOnClickListener(v -> openUserDetail(hod.getUid()));
        return row;
    }

    private static void bindOptionalLine(TextView textView, @Nullable String value) {
        if (value != null && !value.isEmpty()) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(value);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private static String orNotAssigned(@Nullable String value) {
        return value != null && !value.isEmpty() ? value : "—";
    }

    @Nullable
    private static String findDepartmentName(List<Department> departments, @Nullable String id) {
        if (id == null) return null;
        for (Department department : departments) {
            if (department.getId().equals(id)) return department.getName();
        }
        return null;
    }

    private static String findSessionLabel(List<Session> sessions, @Nullable String id) {
        if (id == null) return "";
        for (Session session : sessions) {
            if (session.getId().equals(id)) return session.getLabel();
        }
        return "";
    }

    private static String findSemesterName(List<Semester> semesters, @Nullable String id) {
        if (id == null) return "";
        for (Semester semester : semesters) {
            if (semester.getId().equals(id)) return semester.getDisplayName();
        }
        return "";
    }

    private static String joinDepartmentNames(List<Department> departments, List<String> ids) {
        StringBuilder names = new StringBuilder();
        for (String id : ids) {
            String name = findDepartmentName(departments, id);
            if (name == null) continue;
            if (names.length() > 0) names.append(", ");
            names.append(name);
        }
        return names.toString();
    }

    /** Students only: plain Department chips, single-select, no "All" option (a student browse
     * always needs a specific department picked before Session/Semester can load). */
    private void buildDepartmentChips(LinearLayout chipRow, List<Department> departments, @Nullable String selectedId,
            @ColorRes int accentColorRes, java.util.function.Consumer<String> onSelect) {
        if (departments.isEmpty()) {
            addInfoChip(chipRow, getString(R.string.drilldown_loading));
            return;
        }
        for (Department department : departments) {
            chipRow.addView(buildChip(chipRow, department.getName(), department.getId().equals(selectedId),
                    accentColorRes, v -> onSelect.accept(department.getId())));
        }
    }

    /** Teachers/HODs: an "All" chip first (null selection = no filter) followed by every
     * Department - matches "show everyone first, then optionally filter" from the spec. */
    private void buildDepartmentFilterChips(LinearLayout chipRow, List<Department> departments, @Nullable String selectedId,
            @ColorRes int accentColorRes, java.util.function.Consumer<String> onSelect) {
        chipRow.addView(buildChip(chipRow, getString(R.string.drilldown_all_chip), selectedId == null,
                accentColorRes, v -> onSelect.accept(null)));
        for (Department department : departments) {
            chipRow.addView(buildChip(chipRow, department.getName(), department.getId().equals(selectedId),
                    accentColorRes, v -> onSelect.accept(department.getId())));
        }
    }

    /** Appends one new horizontally-scrolling chip line to a section's stacked drill-down levels
     * (Department on its own line, then Session below it once picked, then Semester below that)
     * and returns the row so the caller can populate it - only this line scrolls horizontally,
     * never the page. */
    private LinearLayout addChipLevelRow(LinearLayout chipRowsContainer, boolean isFirstRow) {
        HorizontalScrollView scrollView = new HorizontalScrollView(requireContext());
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (!isFirstRow) scrollParams.topMargin = dp(8);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        scrollView.addView(row);

        chipRowsContainer.addView(scrollView);
        return row;
    }

    private Chip buildChip(LinearLayout parent, String label, boolean selected, @ColorRes int accentColorRes, View.OnClickListener listener) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setOnClickListener(listener);
        int accent = ContextCompat.getColor(requireContext(), accentColorRes);
        if (selected) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(accent));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surface_variant_color)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant_color));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(6));
        chip.setLayoutParams(params);
        return chip;
    }

    private void addInfoChip(LinearLayout chipRow, String text) {
        TextView info = new TextView(requireContext());
        info.setText(text);
        info.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant_color));
        info.setTextSize(12.5f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(6));
        info.setLayoutParams(params);
        chipRow.addView(info);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void renderPendingApprovals(View view, AdminHomeUiState state) {
        LinearLayout container = view.findViewById(R.id.pendingApprovalsContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View dot = container.getChildAt(i).findViewById(R.id.pillDot);
            if (dot != null) AnimUtils.stopPulse(dot);
        }
        container.removeAllViews();
        List<PendingApprovalRow> rows = state.getPendingApprovals();
        view.findViewById(R.id.textPendingEmpty).setVisibility(
                rows.isEmpty() && !state.isLoading() ? View.VISIBLE : View.GONE);
        for (PendingApprovalRow row : rows) {
            container.addView(buildPendingRow(container, row));
        }
        if (!rows.isEmpty()) container.scheduleLayoutAnimation();
    }

    private View buildPendingRow(LinearLayout parent, PendingApprovalRow row) {
        View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_pending_approval_row, parent, false);

        ((TextView) itemView.findViewById(R.id.textInitials)).setText(initialsOf(row.getFullName()));
        int roleColor = ContextCompat.getColor(requireContext(), AccentColors.colorForRole(row.getRole()));
        itemView.findViewById(R.id.avatarCircle).setBackgroundTintList(ColorStateList.valueOf(roleColor));

        ((TextView) itemView.findViewById(R.id.textName)).setText(row.getFullName());
        String roleLabel = roleLabel(row.getRole());
        String department = row.getDepartmentName();
        String roleDeptLine = department != null && !department.isEmpty()
                ? getString(R.string.pending_role_department_format, roleLabel, department)
                : roleLabel;
        ((TextView) itemView.findViewById(R.id.textRoleDepartment)).setText(roleDeptLine);
        ((TextView) itemView.findViewById(R.id.textDate)).setText(DateUtils.millisToDisplay(row.getCreatedAt()));

        TextView pillStatus = itemView.findViewById(R.id.textPillStatus);
        int warning = ContextCompat.getColor(requireContext(), R.color.status_warning);
        AccentColors.applyPill(pillStatus, R.color.status_warning, getString(R.string.status_pending_label));
        itemView.findViewById(R.id.pillDot).setBackgroundTintList(ColorStateList.valueOf(warning));
        AnimUtils.pulse(itemView.findViewById(R.id.pillDot));

        itemView.findViewById(R.id.buttonView).setOnClickListener(v -> openUserDetail(row.getUid()));
        itemView.findViewById(R.id.buttonApprove).setOnClickListener(v -> viewModel.approve(row.getUid()));
        itemView.findViewById(R.id.buttonReject).setOnClickListener(v -> viewModel.reject(row.getUid()));

        return itemView;
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) initials.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return initials.length() > 0 ? initials.toString() : "?";
    }

    private String roleLabel(UserRole role) {
        if (role == null) return "";
        switch (role) {
            case ADMIN:
                return getString(R.string.role_admin);
            case HOD:
                return getString(R.string.role_hod_upper);
            case TEACHER:
                return getString(R.string.role_teacher);
            case STUDENT:
            default:
                return getString(R.string.role_student);
        }
    }

    private void openUserDetail(String uid) {
        Bundle args = new Bundle();
        args.putString("uid", uid);
        NavHostFragment.findNavController(this).navigate(R.id.adminUserDetailFragment, args);
    }

    private void openUsersTab(int tabIndex, @Nullable String filterName) {
        Bundle args = new Bundle();
        args.putInt("initialTab", tabIndex);
        if (filterName != null) args.putString("initialFilter", filterName);
        NavHostFragment.findNavController(this).navigate(R.id.adminUsersFragment, args);
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
