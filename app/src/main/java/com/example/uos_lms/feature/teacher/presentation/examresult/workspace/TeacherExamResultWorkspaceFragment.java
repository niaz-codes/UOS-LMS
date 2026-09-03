package com.example.uos_lms.feature.teacher.presentation.examresult.workspace;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.DepartmentOptions;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.session.ThemePreferenceManager;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.NotificationBadgeHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.example.uos_lms.core.ui.ThemeToggleHelper;
import com.example.uos_lms.feature.auth.presentation.AuthNavigator;
import com.example.uos_lms.feature.auth.presentation.register.OptionTabGroup;
import com.example.uos_lms.feature.notifications.AppNotificationCenter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Dedicated Teacher "Exam Result" tab (replaces Reports in the bottom nav): tab-style
 * Department -> Session -> Semester -> Subject -> Exam Type selection, a spreadsheet for bulk
 * marks entry with a live 4-card summary (total/entered/passed/failed), sort, Save Draft /
 * Submit for the current & previous exam types, and one-tap repeat-exam submission. */
@AndroidEntryPoint
public class TeacherExamResultWorkspaceFragment extends Fragment {

    @Inject
    AppNotificationCenter notificationCenter;
    @Inject
    ThemePreferenceManager themePreferenceManager;

    private TeacherExamResultWorkspaceViewModel viewModel;
    private TextInputEditText editTotalMarks;
    private boolean suppressTotalMarksWatcher;
    private TextInputLayout totalMarksLayout;
    private List<String> lastRowSignature;

    private OptionTabGroup tabsDepartment;
    private OptionTabGroup tabsSession;
    private OptionTabGroup tabsSemester;
    private OptionTabGroup tabsSubject;
    private OptionTabGroup tabsExamType;
    private RefreshUx.Binding refreshBinding;

    private SimpleListAdapter<TeacherCourseSummary> coursesAdapter;
    private TextInputEditText editSearchStudent;
    private boolean suppressSearchWatcher;
    private ChipGroup chipGroupFilter;
    private List<String> lastCourseSignature;

    public TeacherExamResultWorkspaceFragment() {
        super(R.layout.fragment_teacher_exam_result_workspace);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_exam_result_workspace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherExamResultWorkspaceViewModel.class);

        bindToolbar(view);
        bindBottomNav(view);

        tabsDepartment = view.findViewById(R.id.tabsDepartment);
        tabsSession = view.findViewById(R.id.tabsSession);
        tabsSemester = view.findViewById(R.id.tabsSemester);
        tabsSubject = view.findViewById(R.id.tabsSubject);
        tabsExamType = view.findViewById(R.id.tabsExamType);
        for (OptionTabGroup group : new OptionTabGroup[]{tabsDepartment, tabsSession, tabsSemester,
                tabsSubject, tabsExamType}) {
            group.setThemeAware(true);
        }

        tabsDepartment.setListener((group, ids) -> viewModel.onDepartmentSelected(first(ids)));
        tabsSession.setListener((group, ids) -> viewModel.onSessionSelected(first(ids)));
        tabsSemester.setListener((group, ids) -> viewModel.onSemesterSelected(first(ids)));
        tabsSubject.setListener((group, ids) -> viewModel.onSubjectSelected(first(ids)));
        tabsExamType.setListener((group, ids) -> viewModel.onExamTypeSelected(first(ids)));

        totalMarksLayout = view.findViewById(R.id.totalMarksLayout);
        editTotalMarks = view.findViewById(R.id.editTotalMarks);
        editTotalMarks.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressTotalMarksWatcher) viewModel.updateTotalMarks(s);
        }));

        RecyclerView recyclerRows = view.findViewById(R.id.recyclerRows);
        recyclerRows.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRows.setNestedScrollingEnabled(false);

        SimpleListAdapter<ResultEntryRow> adapter = new SimpleListAdapter<>(R.layout.item_result_entry_row,
                (itemView, row, position) -> bindRow(itemView, row));
        recyclerRows.setAdapter(adapter);

        MaterialButton buttonSaveDraft = view.findViewById(R.id.buttonSaveDraft);
        MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);
        buttonSaveDraft.setOnClickListener(v -> viewModel.saveDraft());
        buttonSubmit.setOnClickListener(v -> viewModel.submit());

        view.findViewById(R.id.buttonRetryOptions).setOnClickListener(v -> viewModel.loadOptions());

        // "My Assigned Courses" grid
        RecyclerView recyclerCourses = view.findViewById(R.id.recyclerCourses);
        recyclerCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCourses.setNestedScrollingEnabled(false);
        coursesAdapter = new SimpleListAdapter<>(R.layout.item_teacher_course_result_card,
                (itemView, course, position) -> bindCourseCard(itemView, course));
        recyclerCourses.setAdapter(coursesAdapter);
        view.findViewById(R.id.buttonRetryCourses).setOnClickListener(v -> viewModel.loadCourseOverview());
        view.findViewById(R.id.buttonBackToCourses).setOnClickListener(v -> viewModel.backToCourseGrid());

        editSearchStudent = view.findViewById(R.id.editSearchStudent);
        editSearchStudent.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressSearchWatcher) viewModel.setStudentSearchQuery(s);
        }));

        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            viewModel.setStudentFilter(filterForChip(checkedIds.get(0)));
        });

        view.findViewById(R.id.buttonSort).setOnClickListener(this::showSortMenu);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            // The department/session/semester/subject cascade (optionsLoading/scrollContent's
            // former full-screen gate) is retired from the visible flow - courses are opened
            // straight from the grid below, which has its own self-contained loading/error UI.
            view.findViewById(R.id.optionsLoading).setVisibility(View.GONE);
            view.findViewById(R.id.scrollContent).setVisibility(View.VISIBLE);

            renderSections(view, state);
            bindSelectionTabs(state);
            renderSummary(view, state);
            renderCourseGrid(view, state);

            boolean sheetActive = !state.isShowCourseGrid() && state.getSelectedSubjectId() != null && !state.isLoadingRoster();
            view.findViewById(R.id.marksheetContainer).setVisibility(sheetActive ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.bottomActionBar).setVisibility(sheetActive ? View.VISIBLE : View.GONE);

            boolean repeat = TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(state.getSelectedExamType());
            totalMarksLayout.setVisibility(repeat ? View.GONE : View.VISIBLE);
            buttonSaveDraft.setVisibility(repeat ? View.GONE : View.VISIBLE);
            buttonSubmit.setText(repeat ? R.string.exam_result_submit_repeat_button : R.string.submit_for_hod_approval_button);

            suppressTotalMarksWatcher = true;
            if (!editTotalMarks.isFocused() && !editTotalMarks.getText().toString().equals(state.getTotalMarksInput())) {
                editTotalMarks.setText(state.getTotalMarksInput());
                editTotalMarks.setSelection(editTotalMarks.getText().length());
            }
            suppressTotalMarksWatcher = false;
            editTotalMarks.setEnabled(!state.isTotalMarksLocked());

            TextView textSheetHint = view.findViewById(R.id.textSheetHint);
            textSheetHint.setText(repeat ? R.string.exam_result_sheet_hint_repeat
                    : TeacherExamResultWorkspaceUiState.EXAM_TYPE_PREVIOUS.equals(state.getSelectedExamType())
                    ? R.string.exam_result_sheet_hint_previous
                    : R.string.exam_result_sheet_hint_current);

            boolean hasRows = !state.getRows().isEmpty();
            List<ResultEntryRow> visibleForEmptyCheck = sheetActive ? state.getVisibleRows() : Collections.emptyList();
            TextView emptySheet = view.findViewById(R.id.emptySheet);
            if (hasRows && visibleForEmptyCheck.isEmpty()) {
                emptySheet.setText(R.string.no_students_found_search);
            } else {
                emptySheet.setText(repeat ? R.string.exam_result_no_repeat_students : R.string.exam_result_no_students);
            }
            emptySheet.setVisibility(sheetActive && visibleForEmptyCheck.isEmpty() && !state.isLoadingRoster() ? View.VISIBLE : View.GONE);

            View searchLayout = view.findViewById(R.id.searchStudentLayout);
            View filterRow = chipGroupFilter != null ? (View) chipGroupFilter.getParent().getParent() : null;
            boolean showRosterTools = sheetActive && hasRows;
            searchLayout.setVisibility(showRosterTools ? View.VISIBLE : View.GONE);
            if (filterRow != null) filterRow.setVisibility(showRosterTools ? View.VISIBLE : View.GONE);

            suppressSearchWatcher = true;
            if (!editSearchStudent.isFocused() && !editSearchStudent.getText().toString().equals(state.getStudentSearchQuery())) {
                editSearchStudent.setText(state.getStudentSearchQuery());
                editSearchStudent.setSelection(editSearchStudent.getText().length());
            }
            suppressSearchWatcher = false;

            int checkedChipId = chipForFilter(state.getStudentFilter());
            if (chipGroupFilter.getCheckedChipId() != checkedChipId) {
                chipGroupFilter.check(checkedChipId);
            }

            View analyticsCard = view.findViewById(R.id.analyticsCard);
            boolean showAnalytics = sheetActive && state.hasAnalytics();
            analyticsCard.setVisibility(showAnalytics ? View.VISIBLE : View.GONE);
            if (showAnalytics) bindAnalytics(view, state);

            if (sheetActive) {
                // Only force a full RecyclerView rebind when the roster's membership/order
                // actually changed (subject/exam-type switch, sort toggle). A marks keystroke
                // only changes values within already-bound rows (updated in place by bindRow's
                // watcher), so resubmitting on every character would notifyDataSetChanged() the
                // list mid-edit and steal focus away from the row the teacher is typing into.
                List<ResultEntryRow> visibleRows = state.getVisibleRows();
                List<String> signature = rowSignature(visibleRows);
                if (!signature.equals(lastRowSignature)) {
                    lastRowSignature = signature;
                    adapter.submitList(visibleRows);
                } else {
                    adapter.setItemsQuietly(visibleRows);
                }
            } else {
                lastRowSignature = null;
            }

            boolean saving = state.isSaving();
            CircularProgressIndicator progressSaving = view.findViewById(R.id.progressSaving);
            buttonSaveDraft.setEnabled(!saving && !repeat);
            buttonSubmit.setEnabled(state.canSubmit() && !saving);
            progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);

            if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            }

            refreshBinding.setRefreshing(viewModel.isBusy());
        });
    }

    private void bindToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ThemeToggleHelper.applyIcon(toolbar.getMenu().findItem(R.id.actionThemeToggle), requireContext());
        BadgeDrawable notificationBadge = NotificationBadgeHelper.attach(requireContext(), toolbar, R.id.actionNotifications);
        notificationCenter.getUnreadCount().observe(getViewLifecycleOwner(),
                count -> NotificationBadgeHelper.update(notificationBadge, count));

        refreshBinding = RefreshUx.bindMenuItem(toolbar.getMenu().findItem(R.id.actionRefresh), null, () -> viewModel.refresh());

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.actionRefresh) {
                refreshBinding.trigger();
                return true;
            }
            if (id == R.id.actionLogout) {
                AuthNavigator.navigateToLoginClearingStack(NavHostFragment.findNavController(this));
                return true;
            }
            if (id == R.id.actionReports) {
                NavHostFragment.findNavController(this).navigate(R.id.teacherReportsFragment);
                return true;
            }
            if (id == R.id.actionAnnouncements) {
                NavHostFragment.findNavController(this).navigate(R.id.announcementsFragment);
                return true;
            }
            if (id == R.id.actionLeave) {
                NavHostFragment.findNavController(this).navigate(R.id.teacherLeaveFragment);
                return true;
            }
            if (id == R.id.actionTimetable) {
                NavHostFragment.findNavController(this).navigate(R.id.teacherTimetableFragment);
                return true;
            }
            if (id == R.id.actionExamSchedule) {
                NavHostFragment.findNavController(this).navigate(R.id.teacherExamScheduleFragment);
                return true;
            }
            if (id == R.id.actionMessaging) {
                NavHostFragment.findNavController(this).navigate(R.id.messagingInboxFragment);
                return true;
            }
            if (id == R.id.actionNotifications) {
                NavHostFragment.findNavController(this).navigate(R.id.notificationsFragment);
                return true;
            }
            if (id == R.id.actionThemeToggle) {
                ThemeToggleHelper.toggle(requireContext(), themePreferenceManager);
                return true;
            }
            return false;
        });
    }

    private void bindBottomNav(View view) {
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
    }

    private void renderSections(View view, TeacherExamResultWorkspaceUiState state) {
        view.findViewById(R.id.sectionSession).setVisibility(state.getSelectedDepartmentId() != null ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sectionSemester).setVisibility(state.getSelectedSessionId() != null ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sectionSubject).setVisibility(state.getSelectedSemesterId() != null ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.sectionExamType).setVisibility(state.getSelectedSubjectId() != null ? View.VISIBLE : View.GONE);

        boolean hasDepartments = state.getDepartmentOptions() != null && !state.getDepartmentOptions().isEmpty();
        View emptyDepartment = view.findViewById(R.id.emptyDepartment);
        emptyDepartment.setVisibility(hasDepartments && state.getDepartmentIds().isEmpty() ? View.VISIBLE : View.GONE);

        View emptySession = view.findViewById(R.id.emptySession);
        emptySession.setVisibility(state.getSelectedDepartmentId() != null && state.getSessionIds().isEmpty() ? View.VISIBLE : View.GONE);

        View emptySemester = view.findViewById(R.id.emptySemester);
        emptySemester.setVisibility(state.getSelectedSessionId() != null && state.getSemesterIds().isEmpty() ? View.VISIBLE : View.GONE);

        View emptySubject = view.findViewById(R.id.emptySubject);
        emptySubject.setVisibility(state.getSelectedSemesterId() != null && state.getSubjectIds().isEmpty() && !state.isLoadingSubjects() ? View.VISIBLE : View.GONE);

        ((CircularProgressIndicator) view.findViewById(R.id.progressSubject)).setVisibility(
                state.isLoadingSubjects() ? View.VISIBLE : View.GONE);
        ((CircularProgressIndicator) view.findViewById(R.id.progressRoster)).setVisibility(
                state.isLoadingRoster() ? View.VISIBLE : View.GONE);

        View errorDepartment = view.findViewById(R.id.errorDepartment);
        errorDepartment.setVisibility(state.getOptionsError() != null ? View.VISIBLE : View.GONE);
        if (state.getOptionsError() != null) ((TextView) errorDepartment).setText(state.getOptionsError());

        View errorSubject = view.findViewById(R.id.errorSubject);
        errorSubject.setVisibility(state.getSectionError() != null && state.getSelectedSubjectId() == null ? View.VISIBLE : View.GONE);
        if (state.getSectionError() != null && state.getSelectedSubjectId() == null) {
            ((TextView) errorSubject).setText(state.getSectionError());
        }
        View errorRoster = view.findViewById(R.id.errorRoster);
        errorRoster.setVisibility(state.getSectionError() != null && state.getSelectedSubjectId() != null ? View.VISIBLE : View.GONE);
        if (state.getSectionError() != null && state.getSelectedSubjectId() != null) {
            ((TextView) errorRoster).setText(state.getSectionError());
        }
    }

    private void bindSelectionTabs(TeacherExamResultWorkspaceUiState state) {
        List<DepartmentOptions> departments = state.getDepartmentOptions();
        if (departments == null) departments = Collections.emptyList();
        List<OptionTabGroup.Option> departmentOptions = new ArrayList<>();
        for (DepartmentOptions department : departments) {
            departmentOptions.add(new OptionTabGroup.Option(department.getDepartment().getId(), department.getDepartment().getName()));
        }
        tabsDepartment.setOptions(departmentOptions);
        tabsDepartment.setSelectedIds(state.getSelectedDepartmentId() == null
                ? Collections.emptyList() : Collections.singletonList(state.getSelectedDepartmentId()));

        List<OptionTabGroup.Option> sessions = new ArrayList<>();
        for (String sessionId : state.getSessionIds()) {
            DepartmentOptions department = state.getSelectedDepartment();
            String label = department == null ? "" : labelFor(sessionId, department);
            sessions.add(new OptionTabGroup.Option(sessionId, label));
        }
        tabsSession.setOptions(sessions);
        tabsSession.setSelectedIds(state.getSelectedSessionId() == null
                ? Collections.emptyList() : Collections.singletonList(state.getSelectedSessionId()));

        List<OptionTabGroup.Option> semesters = new ArrayList<>();
        for (String semesterId : state.getSemesterIds()) {
            semesters.add(new OptionTabGroup.Option(semesterId, labelFor(semesterId, state.getSelectedDepartment())));
        }
        tabsSemester.setOptions(semesters);
        tabsSemester.setSelectedIds(state.getSelectedSemesterId() == null
                ? Collections.emptyList() : Collections.singletonList(state.getSelectedSemesterId()));

        List<OptionTabGroup.Option> subjects = new ArrayList<>();
        for (Subject subject : state.getSubjects()) {
            subjects.add(new OptionTabGroup.Option(subject.getId(), subject.getTitle()));
        }
        tabsSubject.setOptions(subjects);
        tabsSubject.setSelectedIds(state.getSelectedSubjectId() == null
                ? Collections.emptyList() : Collections.singletonList(state.getSelectedSubjectId()));

        List<OptionTabGroup.Option> examTypes = new ArrayList<>();
        examTypes.add(new OptionTabGroup.Option(TeacherExamResultWorkspaceUiState.EXAM_TYPE_CURRENT, getString(R.string.exam_result_type_current)));
        examTypes.add(new OptionTabGroup.Option(TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT, getString(R.string.exam_result_type_repeat)));
        examTypes.add(new OptionTabGroup.Option(TeacherExamResultWorkspaceUiState.EXAM_TYPE_PREVIOUS, getString(R.string.exam_result_type_previous)));
        tabsExamType.setOptions(examTypes);
        tabsExamType.setSelectedIds(Collections.singletonList(state.getSelectedExamType()));
    }

    private static String labelFor(String id, @Nullable DepartmentOptions department) {
        if (department == null) return id;
        if (department.getSessionIds().contains(id)) {
            for (com.example.uos_lms.core.domain.model.Session session : department.getSessions()) {
                if (session.getId().equals(id)) return session.getLabel();
            }
        } else {
            for (com.example.uos_lms.core.domain.model.Semester semester : department.getSemesters()) {
                if (semester.getId().equals(id)) return semester.getDisplayName();
            }
        }
        return id;
    }

    private void renderCourseGrid(View view, TeacherExamResultWorkspaceUiState state) {
        boolean showGrid = state.isShowCourseGrid();
        view.findViewById(R.id.courseGridSection).setVisibility(showGrid ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.courseBreadcrumb).setVisibility(showGrid ? View.GONE : View.VISIBLE);

        if (!showGrid) {
            TeacherCourseSummary active = state.getActiveCourse();
            if (active != null) {
                Subject subject = active.getSubject();
                ((TextView) view.findViewById(R.id.textActiveCourseTitle)).setText(subject.getTitle());
                ((TextView) view.findViewById(R.id.textActiveCourseMeta)).setText(subject.getCode()
                        + " • " + active.getSemesterLabel() + " • " + subject.getCreditHours() + " Credit Hours");
            }
            return;
        }

        boolean loading = state.isLoadingCourses();
        boolean hasError = state.getCoursesError() != null;
        List<TeacherCourseSummary> courses = state.getCourses();
        boolean empty = !loading && !hasError && courses.isEmpty();

        view.findViewById(R.id.progressCourses).setVisibility(loading ? View.VISIBLE : View.GONE);
        TextView errorCourses = view.findViewById(R.id.errorCourses);
        errorCourses.setVisibility(hasError ? View.VISIBLE : View.GONE);
        if (hasError) errorCourses.setText(state.getCoursesError());
        view.findViewById(R.id.buttonRetryCourses).setVisibility(hasError ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.emptyCourses).setVisibility(empty ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.recyclerCourses).setVisibility(!loading && !hasError && !empty ? View.VISIBLE : View.GONE);

        bindStatCard(view.findViewById(R.id.statAssignedCourses), R.drawable.ic_assignment,
                R.color.role_teacher_start, R.color.teacher_container, R.color.on_teacher_container,
                state.getAssignedCourseCount(), R.string.course_summary_assigned);
        bindStatCard(view.findViewById(R.id.statCourseStudents), R.drawable.ic_group,
                R.color.status_success, R.color.student_container, R.color.on_student_container,
                state.getTotalStudentsAcrossCourses(), R.string.course_summary_students);
        bindStatCard(view.findViewById(R.id.statCourseEntered), R.drawable.ic_edit,
                R.color.status_warning, R.color.amber_tertiary_container, R.color.on_amber_tertiary_container,
                state.getResultsEnteredAcrossCourses(), R.string.course_summary_results_entered);
        bindStatCard(view.findViewById(R.id.statCoursePending), R.drawable.ic_hourglass_empty,
                R.color.error_color, R.color.error_container, R.color.on_error_container,
                state.getPendingResultsAcrossCourses(), R.string.course_summary_pending);

        List<String> signature = new ArrayList<>(courses.size());
        for (TeacherCourseSummary course : courses) signature.add(course.getSubject().getId());
        if (!signature.equals(lastCourseSignature)) {
            lastCourseSignature = signature;
            coursesAdapter.submitList(courses);
        } else {
            coursesAdapter.setItemsQuietly(courses);
        }
    }

    private void bindCourseCard(View itemView, TeacherCourseSummary course) {
        Subject subject = course.getSubject();
        ((TextView) itemView.findViewById(R.id.textCourseTitle)).setText(subject.getTitle());
        ((TextView) itemView.findViewById(R.id.textCourseMeta)).setText(
                subject.getCode() + " • " + subject.getCreditHours() + " Credit Hours");
        ((TextView) itemView.findViewById(R.id.textCourseDeptSession)).setText(
                course.getDepartmentName() + " • " + course.getSessionLabel() + " • " + course.getSemesterLabel());
        ((TextView) itemView.findViewById(R.id.textCourseStudents)).setText(String.valueOf(course.getTotalStudents()));
        ((TextView) itemView.findViewById(R.id.textCourseEntered)).setText(
                course.getResultsEntered() + "/" + course.getTotalStudents());
        ((TextView) itemView.findViewById(R.id.textCoursePending)).setText(String.valueOf(course.getResultsPendingEntry()));

        CourseResultStatus status = course.getStatus();
        AccentColors.applyPill(itemView.findViewById(R.id.textCourseStatus), status.colorRes(), getString(status.labelRes()));

        itemView.findViewById(R.id.buttonManageResults).setOnClickListener(v -> viewModel.openCourse(course));
    }

    private void bindAnalytics(View view, TeacherExamResultWorkspaceUiState state) {
        ((TextView) view.findViewById(R.id.textClassAverage)).setText(String.format(Locale.US, "%.1f%%", state.getClassAverage()));
        ((TextView) view.findViewById(R.id.textClassHighest)).setText(String.format(Locale.US, "%.0f%%", state.getHighestPercentage()));
        ((TextView) view.findViewById(R.id.textClassLowest)).setText(String.format(Locale.US, "%.0f%%", state.getLowestPercentage()));

        int passPercent = state.getPassPercent();
        int failPercent = state.getFailPercent();
        View barPass = view.findViewById(R.id.barPassSegment);
        View barFail = view.findViewById(R.id.barFailSegment);
        ((LinearLayout.LayoutParams) barPass.getLayoutParams()).weight = Math.max(passPercent, 0f);
        ((LinearLayout.LayoutParams) barFail.getLayoutParams()).weight = Math.max(failPercent, 0f);
        barPass.requestLayout();
        barFail.requestLayout();

        ((TextView) view.findViewById(R.id.textPassFailPercent)).setText(getString(
                R.string.course_pass_fail_percent_format, passPercent, failPercent,
                state.getEnteredCount(), state.getTotalStudents()));
    }

    private TeacherExamResultWorkspaceUiState.StudentFilter filterForChip(int chipId) {
        if (chipId == R.id.chipFilterEntered) return TeacherExamResultWorkspaceUiState.StudentFilter.ENTERED;
        if (chipId == R.id.chipFilterPending) return TeacherExamResultWorkspaceUiState.StudentFilter.PENDING;
        if (chipId == R.id.chipFilterPassed) return TeacherExamResultWorkspaceUiState.StudentFilter.PASSED;
        if (chipId == R.id.chipFilterFailed) return TeacherExamResultWorkspaceUiState.StudentFilter.FAILED;
        return TeacherExamResultWorkspaceUiState.StudentFilter.ALL;
    }

    private int chipForFilter(TeacherExamResultWorkspaceUiState.StudentFilter filter) {
        switch (filter) {
            case ENTERED: return R.id.chipFilterEntered;
            case PENDING: return R.id.chipFilterPending;
            case PASSED: return R.id.chipFilterPassed;
            case FAILED: return R.id.chipFilterFailed;
            case ALL:
            default: return R.id.chipFilterAll;
        }
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.sort_by_roll);
        menu.getMenu().add(0, 2, 1, R.string.sort_by_name);
        menu.getMenu().add(0, 3, 2, R.string.sort_by_marks);
        menu.getMenu().add(0, 4, 3, R.string.sort_by_grade);
        menu.getMenu().add(0, 5, 4, R.string.sort_by_gpa);
        menu.setOnMenuItemClickListener(item -> {
            TeacherExamResultWorkspaceUiState.StudentSort sort;
            int id = item.getItemId();
            if (id == 2) sort = TeacherExamResultWorkspaceUiState.StudentSort.NAME;
            else if (id == 3) sort = TeacherExamResultWorkspaceUiState.StudentSort.MARKS;
            else if (id == 4) sort = TeacherExamResultWorkspaceUiState.StudentSort.GRADE;
            else if (id == 5) sort = TeacherExamResultWorkspaceUiState.StudentSort.GPA;
            else sort = TeacherExamResultWorkspaceUiState.StudentSort.ROLL;
            viewModel.setStudentSort(sort);
            return true;
        });
        menu.show();
    }

    private void renderSummary(View view, TeacherExamResultWorkspaceUiState state) {
        boolean show = state.getSelectedSubjectId() != null && !state.isLoadingRoster() && !state.getRows().isEmpty();
        view.findViewById(R.id.summaryCard).setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        bindStatCard(view.findViewById(R.id.statTotalStudents), R.drawable.ic_group,
                R.color.role_teacher_start, R.color.teacher_container, R.color.on_teacher_container,
                state.getTotalStudents(), R.string.exam_result_summary_total);
        bindStatCard(view.findViewById(R.id.statPassed), R.drawable.ic_check_single,
                R.color.status_success, R.color.student_container, R.color.on_student_container,
                state.getPassedCount(), R.string.exam_result_summary_passed);

        view.findViewById(R.id.textSummaryRepeatNote).setVisibility(
                TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(state.getSelectedExamType())
                        ? View.VISIBLE : View.GONE);
    }

    private void bindStatCard(View card, @DrawableRes int iconRes, @ColorRes int accentColorRes,
            @ColorRes int containerColorRes, @ColorRes int onContainerColorRes, int value, @StringRes int labelRes) {
        AccentColors.applyBar(card.findViewById(R.id.accentBar), accentColorRes);
        card.findViewById(R.id.iconBackground).setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), containerColorRes)));
        ImageView icon = card.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), onContainerColorRes));
        ((TextView) card.findViewById(R.id.textValue)).setText(String.valueOf(value));
        ((TextView) card.findViewById(R.id.textLabel)).setText(labelRes);
    }

    private static List<String> rowSignature(List<ResultEntryRow> rows) {
        List<String> ids = new ArrayList<>(rows.size());
        for (ResultEntryRow row : rows) ids.add(row.getSource().getStudent().getUid());
        return ids;
    }

    private void bindRow(View itemView, ResultEntryRow row) {
        String examType = viewModel.getUiState().getValue().getSelectedExamType();
        int total = viewModel.getUiState().getValue().getTotalMarksValue();

        String roll = row.getSource().getStudent().getRollNumber();
        ((TextView) itemView.findViewById(R.id.textRoll)).setText(roll != null && !roll.isEmpty() ? roll : "—");

        ((TextView) itemView.findViewById(R.id.textStudentName)).setText(row.getSource().getStudent().getFullName());
        String reg = row.getSource().getStudent().getRegistrationNumber();
        ((TextView) itemView.findViewById(R.id.textStudentReg)).setText(reg != null && !reg.isEmpty()
                ? reg : row.getSource().getStudent().getEmail());

        TextView textPrevious = itemView.findViewById(R.id.textPreviousAttempt);
        boolean repeat = TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType);
        if (repeat && row.getSource().getResult() != null) {
            double previousMarks = row.getSource().getResult().getObtainedMarks();
            String previousGrade = row.getSource().getResult().getGrade() != null
                    ? row.getSource().getResult().getGrade() : "—";
            textPrevious.setText(getString(R.string.exam_result_previous_attempt_format,
                    formatNumber(previousMarks), previousGrade));
            textPrevious.setVisibility(View.VISIBLE);
        } else {
            textPrevious.setVisibility(View.GONE);
        }

        EditText editMarks = itemView.findViewById(R.id.editMarks);
        Object previousWatcher = editMarks.getTag();
        if (previousWatcher instanceof TextWatcher) {
            editMarks.removeTextChangedListener((TextWatcher) previousWatcher);
        }
        // Never touch text/selection on a field the teacher is actively typing into - avoids
        // stealing the cursor away mid-entry (see the RecyclerView rebind guard above).
        if (!editMarks.isFocused() && !editMarks.getText().toString().equals(row.getMarksInput())) {
            editMarks.setText(row.getMarksInput());
            editMarks.setSelection(editMarks.getText().length());
        }
        boolean editable = row.isEditable(examType);
        editMarks.setEnabled(editable);
        editMarks.setBackgroundResource(editable ? R.drawable.bg_rounded_input : 0);
        String studentUid = row.getSource().getStudent().getUid();
        TextWatcher watcher = new SimpleTextWatcher(s -> {
            viewModel.updateMarks(studentUid, s);
            // Recompute this row's own grade/GPA/status directly in place instead of waiting
            // for a RecyclerView rebind, so live feedback never requires notifyDataSetChanged()
            // while this row's EditText holds focus.
            int currentTotal = viewModel.getUiState().getValue().getTotalMarksValue();
            applyComputedColumns(itemView, row.toBuilder().marksInput(s).build(), examType, currentTotal);
        });
        editMarks.addTextChangedListener(watcher);
        editMarks.setTag(watcher);

        applyComputedColumns(itemView, row, examType, total);
    }

    private void applyComputedColumns(View itemView, ResultEntryRow row, String examType, int total) {
        ((TextView) itemView.findViewById(R.id.textGrade)).setText(row.displayGrade(examType, total));
        double gpa = row.displayGpa(examType, total);
        ((TextView) itemView.findViewById(R.id.textGpa)).setText(gpa <= 0 ? "—" : String.format(Locale.US, "%.2f", gpa));
        bindStatus((TextView) itemView.findViewById(R.id.textStatus), row, examType, total);

        // Non-empty but out-of-range marks (e.g. typed 150 against a total of 100) are flagged
        // in red immediately, live as the teacher types - never silently accepted. Empty input
        // (not reached yet) stays neutral.
        EditText editMarks = itemView.findViewById(R.id.editMarks);
        boolean editable = row.isEditable(examType);
        boolean invalidNonEmpty = editable && !row.getMarksInput().isEmpty() && !row.hasValidMarks(examType, total);
        editMarks.setTextColor(ContextCompat.getColor(itemView.getContext(), invalidNonEmpty ? R.color.error_color
                : editable ? R.color.on_surface_color : R.color.on_surface_variant_color));
    }

    private void bindStatus(TextView textStatus, ResultEntryRow row, String examType, int total) {
        if (TeacherExamResultWorkspaceUiState.EXAM_TYPE_REPEAT.equals(examType)) {
            RepeatExam repeat = row.getSource().getRepeatExam();
            if (repeat == null) {
                textStatus.setText("—");
                textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant_color));
                return;
            }
            switch (repeat.getRepeatStatus()) {
                case APPROVED:
                    textStatus.setText(R.string.exam_result_status_approved);
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_success));
                    break;
                case SUBMITTED:
                    textStatus.setText(R.string.exam_result_status_pending);
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning));
                    break;
                case REJECTED:
                    textStatus.setText(R.string.exam_result_status_rejected);
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color));
                    break;
                default:
                    textStatus.setText(R.string.exam_result_status_draft);
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_info));
                    break;
            }
            return;
        }
        String grade = row.displayGrade(examType, total);
        if ("—".equals(grade)) {
            textStatus.setText("—");
            textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant_color));
            return;
        }
        boolean failed = "F".equals(grade);
        textStatus.setText(failed ? R.string.exam_result_status_fail : R.string.exam_result_status_pass);
        textStatus.setTextColor(ContextCompat.getColor(requireContext(),
                failed ? R.color.error_color : R.color.status_success));
    }

    private static String first(List<String> ids) {
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static String formatNumber(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(Locale.US, "%.2f", value);
    }

    private interface TextChangedCallback {
        void onChanged(String text);
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextChangedCallback callback;

        SimpleTextWatcher(TextChangedCallback callback) {
            this.callback = callback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            callback.onChanged(s.toString());
        }
    }
}
