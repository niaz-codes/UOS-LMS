package com.example.uos_lms.feature.student.presentation.dashboard;

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
import com.example.uos_lms.core.domain.model.Subject;
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
public class StudentDashboardFragment extends Fragment {

    @Inject
    ThemePreferenceManager themePreferenceManager;
    @Inject
    AppNotificationCenter notificationCenter;

    private StudentDashboardViewModel viewModel;
    private boolean subjectsAnimated;
    private RefreshUx.Binding refreshBinding;

    public StudentDashboardFragment() {
        super(R.layout.fragment_student_dashboard);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentDashboardViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ThemeToggleHelper.applyIcon(toolbar.getMenu().findItem(R.id.actionThemeToggle), requireContext());
        BadgeDrawable notificationBadge = NotificationBadgeHelper.attach(requireContext(), toolbar, R.id.actionNotifications);
        notificationCenter.getUnreadCount().observe(getViewLifecycleOwner(), count -> NotificationBadgeHelper.update(notificationBadge, count));

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshBinding = RefreshUx.bindMenuItem(toolbar.getMenu().findItem(R.id.actionRefresh), swipeRefresh, () -> viewModel.refresh());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionRefresh) {
                refreshBinding.trigger();
                return true;
            }
            if (item.getItemId() == R.id.actionLogout) {
                viewModel.logout();
                AuthNavigator.navigateToLoginClearingStack(NavHostFragment.findNavController(this));
                return true;
            }
            if (item.getItemId() == R.id.actionCalendar) {
                NavHostFragment.findNavController(this).navigate(R.id.academicCalendarFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionAnnouncements) {
                NavHostFragment.findNavController(this).navigate(R.id.announcementsFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionLeave) {
                NavHostFragment.findNavController(this).navigate(R.id.studentLeaveFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionTimetable) {
                NavHostFragment.findNavController(this).navigate(R.id.studentTimetableFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionExamSchedule) {
                NavHostFragment.findNavController(this).navigate(R.id.studentExamScheduleFragment);
                return true;
            }
            if (item.getItemId() == R.id.actionMessaging) {
                NavHostFragment.findNavController(this).navigate(R.id.messagingInboxFragment);
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
            return false;
        });

        View header = view.findViewById(R.id.gradientHeader);
        ((TextView) header.findViewById(R.id.textGreeting)).setText(R.string.welcome_back_admin);

        View emptyState = view.findViewById(R.id.emptyState);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navHome);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) return true;
            if (id == R.id.navAttendance) NavHostFragment.findNavController(this).navigate(R.id.studentAttendanceFragment);
            else if (id == R.id.navSubmissions) NavHostFragment.findNavController(this).navigate(R.id.studentSubmissionsFragment);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.studentResultsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, header, emptyState, state));
    }

    private void render(View view, View header, View emptyState, StudentDashboardUiState state) {
        ((com.google.android.material.appbar.MaterialToolbar) view.findViewById(R.id.toolbar))
                .setTitle(state.getSemesterLabel().isBlank() ? getString(R.string.student_dashboard_fallback_title) : state.getSemesterLabel());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        boolean noDepartment = !state.isLoading() && state.getDepartmentId() == null;
        boolean noSemester = !state.isLoading() && !noDepartment && state.getSemesterId() == null;

        progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

        TextView textEmptyMessage = emptyState.findViewById(R.id.textEmptyMessage);
        if (noDepartment) {
            textEmptyMessage.setText(R.string.no_department_assigned_message);
        } else if (noSemester) {
            textEmptyMessage.setText(R.string.no_semester_assigned_message);
        }
        emptyState.setVisibility(noDepartment || noSemester ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(!state.isLoading() && !noDepartment && !noSemester ? View.VISIBLE : View.GONE);
        if (state.isLoading() || noDepartment || noSemester) return;

        ((TextView) header.findViewById(R.id.textTitle)).setText(state.getFullName());
        ((TextView) header.findViewById(R.id.textSubtitle)).setText(state.getDepartmentName());

        LinearLayout subjectsContainer = view.findViewById(R.id.subjectsContainer);
        androidx.core.widget.NestedScrollView scrollContainer = (androidx.core.widget.NestedScrollView) contentContainer;

        bindOverviewCard(view, R.id.statSubjects, R.drawable.ic_school, getString(R.string.stat_subjects),
                R.color.role_student_start, R.color.student_container, R.color.on_student_container,
                String.valueOf(state.getSubjects().size()),
                v -> scrollContainer.smoothScrollTo(0, subjectsContainer.getTop()));

        bindOverviewCard(view, R.id.statAttendance, R.drawable.ic_event_available, getString(R.string.stat_attendance_percent),
                R.color.status_info, R.color.teacher_container, R.color.on_teacher_container,
                state.getAttendancePercentage() + "%",
                v -> NavHostFragment.findNavController(this).navigate(R.id.studentAttendanceFragment));

        bindOverviewCard(view, R.id.statAssignments, R.drawable.ic_assignment, getString(R.string.stat_assignments),
                R.color.status_warning, R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                String.valueOf(state.getPendingAssignments()),
                v -> NavHostFragment.findNavController(this).navigate(R.id.studentSubmissionsFragment));

        bindOverviewCard(view, R.id.statResults, R.drawable.ic_grade, getString(R.string.stat_results),
                R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container,
                getString(R.string.view_action_label),
                v -> NavHostFragment.findNavController(this).navigate(R.id.studentResultsFragment));

        view.findViewById(R.id.textNoSubjects).setVisibility(state.getSubjects().isEmpty() ? View.VISIBLE : View.GONE);

        subjectsContainer.removeAllViews();
        for (Subject subject : state.getSubjects()) {
            subjectsContainer.addView(buildSubjectCard(subjectsContainer, subject));
        }
        if (!state.getSubjects().isEmpty() && !subjectsAnimated) {
            subjectsAnimated = true;
            subjectsContainer.scheduleLayoutAnimation();
        }

        refreshBinding.setRefreshing(state.isRefreshing());
        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    /** Premium Overview stat card (Admin panel's card style) - clickable, no trend row since
     * these values aren't month-over-month deltas. */
    private void bindOverviewCard(View root, int includeId, int iconRes, String label,
            int accentColorRes, int containerColorRes, int onContainerColorRes, String value, View.OnClickListener listener) {
        View card = root.findViewById(includeId);
        int accentColor = androidx.core.content.ContextCompat.getColor(requireContext(), accentColorRes);
        int containerColor = androidx.core.content.ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainerColor = androidx.core.content.ContextCompat.getColor(requireContext(), onContainerColorRes);

        com.google.android.material.card.MaterialCardView cardSurface = card.findViewById(R.id.cardSurface);
        cardSurface.setCardBackgroundColor(containerColor);
        cardSurface.setStrokeColor(android.content.res.ColorStateList.valueOf(AccentColors.withAlpha(accentColor, 0.35f)));
        cardSurface.setOnClickListener(listener);
        com.example.uos_lms.core.ui.AnimUtils.applyPressScale(cardSurface);

        card.findViewById(R.id.glowHalo).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        card.findViewById(R.id.decorShapeLarge).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainerColor));
        card.findViewById(R.id.decorShapeSmall).setBackgroundTintList(android.content.res.ColorStateList.valueOf(onContainerColor));

        card.findViewById(R.id.iconBackground).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        android.widget.ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white));

        TextView textLabel = card.findViewById(R.id.textLabel);
        textLabel.setText(label);
        textLabel.setTextColor(onContainerColor);

        TextView textValue = card.findViewById(R.id.textValue);
        textValue.setText(value);
        textValue.setTextColor(onContainerColor);

        View trendRow = (View) card.findViewById(R.id.textTrend).getParent();
        trendRow.setVisibility(View.GONE);
    }

    private View buildSubjectCard(LinearLayout parent, Subject subject) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_subject_card, parent, false);

        ((TextView) card.findViewById(R.id.textTitle)).setText(subject.getTitle());
        ((TextView) card.findViewById(R.id.textCodeCredits)).setText(subject.getCode() + " • " + subject.getCreditHours() + " credit hours");
        ((TextView) card.findViewById(R.id.textTeacher)).setText(subject.getTeacherName() != null
                ? getString(R.string.teacher_label_format, subject.getTeacherName())
                : getString(R.string.teacher_not_assigned));
        com.example.uos_lms.core.ui.AccentColors.applyBar(card.findViewById(R.id.accentBar), R.color.role_student_start);

        card.findViewById(R.id.chipAttendance).setOnClickListener(v -> {
            Bundle extras = new Bundle();
            extras.putString("subjectId", subject.getId());
            NavHostFragment.findNavController(this).navigate(R.id.studentSubjectAttendanceFragment, extras);
        });
        card.findViewById(R.id.chipAssignments).setOnClickListener(v -> {
            Bundle extras = new Bundle();
            extras.putString("subjectId", subject.getId());
            NavHostFragment.findNavController(this).navigate(R.id.studentSubjectAssignmentsFragment, extras);
        });
        card.findViewById(R.id.chipQuizzes).setOnClickListener(v -> {
            Bundle extras = new Bundle();
            extras.putString("subjectId", subject.getId());
            NavHostFragment.findNavController(this).navigate(R.id.studentSubjectQuizzesFragment, extras);
        });
        card.findViewById(R.id.chipMaterials).setOnClickListener(v -> {
            Bundle extras = new Bundle();
            extras.putString("subjectId", subject.getId());
            NavHostFragment.findNavController(this).navigate(R.id.studentSubjectMaterialsFragment, extras);
        });

        return card;
    }
}
