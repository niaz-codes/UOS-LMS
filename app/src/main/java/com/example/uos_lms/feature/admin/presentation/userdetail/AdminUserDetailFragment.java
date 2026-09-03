package com.example.uos_lms.feature.admin.presentation.userdetail;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.domain.model.UserRole;
import com.example.uos_lms.core.domain.model.UserStatus;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.MultiSelectDialogHelper;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminUserDetailFragment extends Fragment {

    @Inject
    SessionManager sessionManager;

    private AdminUserDetailViewModel viewModel;
    private HodConflict lastShownConflict;

    public AdminUserDetailFragment() {
        super(R.layout.fragment_admin_user_detail);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminUserDetailViewModel.class);

        view.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        setLabel(view, R.id.rowEmail, R.string.email);
        setLabel(view, R.id.rowAccountStatus, R.string.label_account_status);
        setLabel(view, R.id.rowRegisteredOn, R.string.label_registered_on);
        setLabel(view, R.id.rowFatherName, R.string.father_name);
        setLabel(view, R.id.rowCnic, R.string.cnic);
        setLabel(view, R.id.rowPhone, R.string.phone);
        setLabel(view, R.id.rowRole, R.string.label_role);

        view.findViewById(R.id.buttonEditProfile).setOnClickListener(v -> {
            User user = currentUser();
            if (user != null) showEditProfileDialog(user);
        });
        view.findViewById(R.id.buttonChangeRole).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.change_role),
                        UserRole.REGISTERABLE_ROLES, UserRole::name, "", role -> viewModel.changeRole(role)));

        view.findViewById(R.id.buttonResetPassword).setOnClickListener(v -> viewModel.resetPassword());
        view.findViewById(R.id.buttonDeleteUser).setOnClickListener(v -> {
            User user = currentUser();
            if (user == null) return;
            ConfirmDialogHelper.show(requireContext(), getString(R.string.delete_user_title),
                    getString(R.string.delete_user_message, user.getFullName()), getString(R.string.delete),
                    () -> viewModel.delete());
        });

        viewModel.getDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (deleted) NavHostFragment.findNavController(this).popBackStack();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private User currentUser() {
        return viewModel.getUiState().getValue().getUser();
    }

    private void render(View view, AdminUserDetailUiState state) {
        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        User user = state.getUser();

        boolean showLoading = state.isLoading() || user == null;
        progressLoading.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(showLoading ? View.GONE : View.VISIBLE);
        if (user == null) return;

        UserAvatarHelper.bind(view.findViewById(R.id.imageAvatar), user.getProfilePhotoUrl());
        ((TextView) view.findViewById(R.id.textName)).setText(user.getFullName());
        ((TextView) view.findViewById(R.id.textEmail)).setText(user.getEmail());
        bindProfileCard(view, user.getRole());

        bindOverview(view, state, user);
        bindAssignedDepartmentSection(view, state, user);
        bindAssignedCoursesSection(view, state, user);

        setValue(view, R.id.rowEmail, user.getEmail());
        StatusChipHelper.bind((TextView) view.findViewById(R.id.rowAccountStatus).findViewById(R.id.textValue), user.getStatus());
        setValue(view, R.id.rowRegisteredOn, user.getCreatedAt() > 0
                ? DateFormat.getMediumDateFormat(requireContext()).format(new Date(user.getCreatedAt()))
                : getString(R.string.not_assigned));
        setValue(view, R.id.rowFatherName, user.getFatherName());
        setValue(view, R.id.rowCnic, user.getCnic());
        setValue(view, R.id.rowPhone, user.getPhone());
        setValue(view, R.id.rowRole, user.getRole().name());

        CachedSession session = sessionManager.getCachedSession().getValue();
        boolean isSelf = session != null && session.getUid().equals(user.getUid());

        // Mirrors the backend's own self-protection guard: an Admin can never delete, suspend,
        // reject, or change the role of their own account (the accidental self-deletion this
        // protects against is exactly why it exists). Editing profile fields and resetting your
        // own password stay available - neither can lock the account out.
        view.findViewById(R.id.buttonDeleteUser).setVisibility(isSelf ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.buttonChangeRole).setVisibility(isSelf ? View.GONE : View.VISIBLE);
        buildStatusActions((LinearLayout) view.findViewById(R.id.statusActionsContainer), user.getStatus(), isSelf);

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessages();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeMessages();
        }

        if (state.getHodConflict() != null) {
            if (state.getHodConflict() != lastShownConflict) {
                lastShownConflict = state.getHodConflict();
                HodConflict conflict = state.getHodConflict();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.replace_hod_title)
                        .setMessage(getString(R.string.replace_hod_message_format,
                                conflict.getExistingHodName(), conflict.getDepartmentName(), user.getFullName()))
                        .setPositiveButton(R.string.reassign, (d, w) -> viewModel.confirmHodReassignment())
                        .setNegativeButton(R.string.cancel_button, (d, w) -> viewModel.cancelHodConflict())
                        .setOnCancelListener(d -> viewModel.cancelHodConflict())
                        .show();
            }
        } else {
            lastShownConflict = null;
        }
    }

    private String roleLabel(UserRole role) {
        String name = role.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.getDefault());
    }

    // ---- Profile card ----

    /** Tints the profile card's avatar ring and role badge to the viewed user's own role color
     * (the same role_admin/hod/teacher/student palette used everywhere else in the app), and
     * gives the card itself a very soft matching wash instead of a flat neutral background. */
    private void bindProfileCard(View view, UserRole role) {
        int accent = ContextCompat.getColor(requireContext(), roleAccentColorRes(role));
        int container = ContextCompat.getColor(requireContext(), roleContainerColorRes(role));
        int onContainer = ContextCompat.getColor(requireContext(), roleOnContainerColorRes(role));

        ((com.google.android.material.card.MaterialCardView) view.findViewById(R.id.cardProfile))
                .setCardBackgroundColor(com.example.uos_lms.core.ui.AccentColors.withAlpha(container, 0.55f));
        view.findViewById(R.id.avatarRing).setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));

        TextView textRole = view.findViewById(R.id.textRole);
        com.example.uos_lms.core.ui.AccentColors.applyPill(textRole, roleAccentColorRes(role), roleLabel(role).toUpperCase(Locale.getDefault()));
        textRole.setTextColor(onContainer);
    }

    @androidx.annotation.ColorRes
    private static int roleAccentColorRes(UserRole role) {
        switch (role) {
            case ADMIN: return R.color.role_admin_start;
            case HOD: return R.color.role_hod_start;
            case TEACHER: return R.color.role_teacher_start;
            case STUDENT:
            default: return R.color.role_student_start;
        }
    }

    @androidx.annotation.ColorRes
    private static int roleContainerColorRes(UserRole role) {
        switch (role) {
            case ADMIN: return R.color.admin_container;
            case HOD: return R.color.hod_container;
            case TEACHER: return R.color.teacher_container;
            case STUDENT:
            default: return R.color.student_container;
        }
    }

    @androidx.annotation.ColorRes
    private static int roleOnContainerColorRes(UserRole role) {
        switch (role) {
            case ADMIN: return R.color.on_admin_container;
            case HOD: return R.color.on_hod_container;
            case TEACHER: return R.color.on_teacher_container;
            case STUDENT:
            default: return R.color.on_student_container;
        }
    }

    // ---- Overview (Status / Role / Department / Session-or-Designation premium cards) ----

    private void bindOverview(View view, AdminUserDetailUiState state, User user) {
        bindPremiumCard(view.findViewById(R.id.statStatus), R.drawable.ic_badge, getString(R.string.stat_label_status),
                statusAccentColorRes(user.getStatus()), statusContainerColorRes(user.getStatus()), statusOnContainerColorRes(user.getStatus()),
                user.getStatus().name());

        bindPremiumCard(view.findViewById(R.id.statRole), R.drawable.ic_person_role, getString(R.string.stat_label_role),
                roleAccentColorRes(user.getRole()), roleContainerColorRes(user.getRole()), roleOnContainerColorRes(user.getRole()),
                roleLabel(user.getRole()));

        boolean showRemainingCards = user.getRole() != UserRole.ADMIN;
        view.findViewById(R.id.statDept).setVisibility(showRemainingCards ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.statSession).setVisibility(showRemainingCards ? View.VISIBLE : View.GONE);
        if (!showRemainingCards) return;

        String deptValue;
        if (user.getRole() == UserRole.TEACHER) {
            int count = user.getDepartmentIds().size();
            Department single = count == 1 ? findById(state.getDepartments(), user.getDepartmentIds().get(0)) : null;
            deptValue = count == 0 ? getString(R.string.not_assigned)
                    : count == 1 ? (single != null ? single.getName() : getString(R.string.not_assigned))
                    : getString(R.string.departments_count_format, count);
        } else {
            Department department = findById(state.getDepartments(), user.getDepartment());
            deptValue = department != null ? department.getName() : getString(R.string.not_assigned);
        }
        bindPremiumCard(view.findViewById(R.id.statDept), R.drawable.ic_school, getString(R.string.stat_label_department),
                R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container, deptValue);

        View fourthCard = view.findViewById(R.id.statSession);
        if (user.getRole() == UserRole.STUDENT) {
            Session activeSession = findSessionById(state.getSessions(), user.getSessionId());
            bindPremiumCard(fourthCard, R.drawable.ic_calendar_month, getString(R.string.stat_label_session),
                    R.color.role_student_start, R.color.student_container, R.color.on_student_container,
                    activeSession != null ? activeSession.getLabel() : getString(R.string.not_assigned));
        } else {
            bindPremiumCard(fourthCard, R.drawable.ic_badge, getString(R.string.stat_label_designation),
                    R.color.role_student_start, R.color.student_container, R.color.on_student_container,
                    user.getDesignation() != null && !user.getDesignation().isEmpty() ? user.getDesignation() : getString(R.string.not_assigned));
        }
    }

    @androidx.annotation.ColorRes
    private static int statusAccentColorRes(UserStatus status) {
        switch (status) {
            case PENDING: return R.color.status_warning;
            case REJECTED:
            case SUSPENDED: return R.color.error_color;
            default: return R.color.status_success;
        }
    }

    @androidx.annotation.ColorRes
    private static int statusContainerColorRes(UserStatus status) {
        switch (status) {
            case PENDING: return R.color.amber_tertiary_container;
            case REJECTED:
            case SUSPENDED: return R.color.error_container;
            default: return R.color.student_container;
        }
    }

    @androidx.annotation.ColorRes
    private static int statusOnContainerColorRes(UserStatus status) {
        switch (status) {
            case PENDING: return R.color.on_amber_tertiary_container;
            case REJECTED:
            case SUSPENDED: return R.color.on_error_container;
            default: return R.color.on_student_container;
        }
    }

    /** Same "premium overview card" component the Admin/Teacher/HOD/Student dashboards use
     * (colored container, glow halo, decorative shapes, icon badge) - reused here instead of
     * a plain outlined card so this screen's Overview matches the rest of the app's dashboards.
     * The trend/sparkline row that numeric dashboard stats use doesn't apply to identity
     * values (Status/Role/Department/Session), so it's hidden rather than left blank. */
    private void bindPremiumCard(View card, @DrawableRes int iconRes, String label,
            @androidx.annotation.ColorRes int accentColorRes, @androidx.annotation.ColorRes int containerColorRes,
            @androidx.annotation.ColorRes int onContainerColorRes, String value) {
        int accentColor = ContextCompat.getColor(requireContext(), accentColorRes);
        int containerColor = ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainerColor = ContextCompat.getColor(requireContext(), onContainerColorRes);

        com.google.android.material.card.MaterialCardView cardSurface = card.findViewById(R.id.cardSurface);
        cardSurface.setCardBackgroundColor(containerColor);
        cardSurface.setStrokeColor(android.content.res.ColorStateList.valueOf(
                com.example.uos_lms.core.ui.AccentColors.withAlpha(accentColor, 0.35f)));

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
        textValue.setTextSize(16f);

        // No trend/sparkline for identity values - hide the whole row rather than show it blank.
        View textTrend = card.findViewById(R.id.textTrend);
        ((View) textTrend.getParent()).setVisibility(View.GONE);
    }

    // ---- Assigned Department (+ Session/Semester for Student) ----

    private void bindAssignedDepartmentSection(View view, AdminUserDetailUiState state, User user) {
        boolean showSection = user.getRole() == UserRole.STUDENT || user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.HOD;
        view.findViewById(R.id.sectionAssignedDepartment).setVisibility(showSection ? View.VISIBLE : View.GONE);
        if (!showSection) return;

        LinearLayout row = view.findViewById(R.id.rowDepartmentPills);
        row.removeAllViews();

        if (user.getRole() == UserRole.TEACHER) {
            for (String departmentId : user.getDepartmentIds()) {
                Department department = findById(state.getDepartments(), departmentId);
                String label = department != null ? department.getName() : departmentId;
                addPill(row, R.drawable.ic_school, label, R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container,
                        v -> confirmRemoveDepartment(user, departmentId, label));
            }
            addPill(row, R.drawable.ic_add, getString(R.string.add_department_pill), v ->
                    MultiSelectDialogHelper.show(requireContext(), getString(R.string.assign_departments),
                            state.getDepartments(), Department::getName, Department::getId,
                            new HashSet<>(user.getDepartmentIds()), getString(R.string.no_departments_exist),
                            viewModel::assignDepartments));
        } else {
            Department department = findById(state.getDepartments(), user.getDepartment());
            String label = department != null ? department.getName() : getString(R.string.not_assigned);
            addPill(row, R.drawable.ic_school, label, R.color.role_hod_start, R.color.hod_container, R.color.on_hod_container, v ->
                    SelectDialogHelper.show(requireContext(), getString(R.string.assign_department),
                            state.getDepartments(), Department::getName,
                            getString(R.string.no_departments_exist), viewModel::requestAssignDepartment));
        }

        boolean showStudentRow = user.getRole() == UserRole.STUDENT && user.getDepartment() != null;
        view.findViewById(R.id.rowSessionSemester).setVisibility(showStudentRow ? View.VISIBLE : View.GONE);
        if (!showStudentRow) return;

        Session activeSession = findSessionById(state.getSessions(), user.getSessionId());
        tintStaticPill(view.findViewById(R.id.pillSession), R.id.imageSessionPillIcon, R.id.textSessionPill,
                activeSession != null ? activeSession.getLabel() : getString(R.string.assign_session));
        view.findViewById(R.id.pillSession).setOnClickListener(v -> {
            List<Session> activeSessions = new ArrayList<>();
            for (Session candidate : state.getSessions()) {
                if (candidate.isActive()) activeSessions.add(candidate);
            }
            SelectDialogHelper.show(requireContext(), getString(R.string.assign_session),
                    activeSessions, Session::getLabel, getString(R.string.no_active_sessions), viewModel::assignSession);
        });

        Semester semester = findSemesterById(state.getSemesters(), user.getSemester());
        tintStaticPill(view.findViewById(R.id.pillSemester), R.id.imageSemesterPillIcon, R.id.textSemesterPill,
                semester != null ? semester.getDisplayName() : getString(R.string.assign_semester));
        view.findViewById(R.id.pillSemester).setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.assign_semester),
                        state.getSemesters(), Semester::getDisplayName,
                        getString(R.string.no_semesters_exist), viewModel::assignSemester));
    }

    private void confirmRemoveDepartment(User user, String departmentId, String label) {
        ConfirmDialogHelper.show(requireContext(), getString(R.string.remove_department_title),
                getString(R.string.remove_department_message, label), getString(R.string.remove), () -> {
                    List<String> remaining = new ArrayList<>(user.getDepartmentIds());
                    remaining.remove(departmentId);
                    viewModel.assignDepartments(remaining);
                });
    }

    // ---- Assigned Courses (Teacher) ----

    private void bindAssignedCoursesSection(View view, AdminUserDetailUiState state, User user) {
        boolean showSection = user.getRole() == UserRole.TEACHER;
        view.findViewById(R.id.sectionAssignedCourses).setVisibility(showSection ? View.VISIBLE : View.GONE);
        if (!showSection) return;

        view.findViewById(R.id.progressCourses).setVisibility(state.isLoadingCourses() ? View.VISIBLE : View.GONE);
        boolean empty = !state.isLoadingCourses() && state.getAssignedCourses().isEmpty();
        view.findViewById(R.id.emptyCoursesProfile).setVisibility(empty ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.scrollCourses).setVisibility(!state.isLoadingCourses() ? View.VISIBLE : View.GONE);

        LinearLayout row = view.findViewById(R.id.rowCoursePills);
        row.removeAllViews();
        for (Subject subject : state.getAssignedCourses()) {
            addPill(row, R.drawable.ic_assignment, subject.getTitle(), R.color.role_teacher_start, R.color.teacher_container, R.color.on_teacher_container, v ->
                    ConfirmDialogHelper.show(requireContext(), getString(R.string.remove_course_title),
                            getString(R.string.remove_course_message, subject.getTitle()), getString(R.string.remove),
                            () -> viewModel.removeCourse(subject)));
        }
        addPill(row, R.drawable.ic_add, getString(R.string.add_course), v -> startAssignCourseFlow());
    }

    /** Plain outlined pill - used only for the trailing "+Add" action, so it visually reads as
     * an action rather than a piece of data. */
    private void addPill(LinearLayout container, @DrawableRes int iconRes, String label, View.OnClickListener onClick) {
        View pill = LayoutInflater.from(requireContext()).inflate(R.layout.item_horizontal_pill, container, false);
        ((ImageView) pill.findViewById(R.id.imagePillIcon)).setImageResource(iconRes);
        ((TextView) pill.findViewById(R.id.textPillLabel)).setText(label);
        pill.setOnClickListener(onClick);
        container.addView(pill);
    }

    /** Colored-fill pill - used for actual assigned data (a department, a course), so the row
     * reads as a set of premium chips rather than plain outlined boxes. */
    private void addPill(LinearLayout container, @DrawableRes int iconRes, String label,
            @androidx.annotation.ColorRes int accentColorRes, @androidx.annotation.ColorRes int containerColorRes,
            @androidx.annotation.ColorRes int onContainerColorRes, View.OnClickListener onClick) {
        View pill = LayoutInflater.from(requireContext()).inflate(R.layout.item_horizontal_pill, container, false);
        int accent = ContextCompat.getColor(requireContext(), accentColorRes);
        int containerColor = ContextCompat.getColor(requireContext(), containerColorRes);
        int onContainerColor = ContextCompat.getColor(requireContext(), onContainerColorRes);

        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) pill;
        card.setCardBackgroundColor(containerColor);
        card.setStrokeColor(android.content.res.ColorStateList.valueOf(
                com.example.uos_lms.core.ui.AccentColors.withAlpha(accent, 0.4f)));

        ImageView icon = pill.findViewById(R.id.imagePillIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(onContainerColor);

        TextView textLabel = pill.findViewById(R.id.textPillLabel);
        textLabel.setText(label);
        textLabel.setTextColor(onContainerColor);

        pill.setOnClickListener(onClick);
        container.addView(pill);
    }

    /** Colors a static (XML-declared, not dynamically inflated) pill card the same way
     * addPill's colored variant does - used for the Student-only Session/Semester pills, which
     * keep their fixed icon from XML and only need their text + colors bound. */
    private void tintStaticPill(View pillCard, int iconViewId, int textViewId, String value) {
        int accent = ContextCompat.getColor(requireContext(), R.color.role_student_start);
        int container = ContextCompat.getColor(requireContext(), R.color.student_container);
        int onContainer = ContextCompat.getColor(requireContext(), R.color.on_student_container);

        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) pillCard;
        card.setCardBackgroundColor(container);
        card.setStrokeColor(android.content.res.ColorStateList.valueOf(
                com.example.uos_lms.core.ui.AccentColors.withAlpha(accent, 0.4f)));

        ((ImageView) pillCard.findViewById(iconViewId)).setColorFilter(onContainer);
        TextView textView = pillCard.findViewById(textViewId);
        textView.setText(value);
        textView.setTextColor(onContainer);
    }

    /** Department -> Semester -> Subject, each step fetched fresh from the backend right before
     * showing its dialog - no persistent cascade state, this is a one-shot assignment action. */
    private void startAssignCourseFlow() {
        viewModel.listDepartmentsForCourseAssign().addOnSuccessListener(departments ->
                SelectDialogHelper.show(requireContext(), getString(R.string.select_department),
                        departments, Department::getName, getString(R.string.no_departments_exist),
                        department -> viewModel.listSemestersForCourseAssign(department.getId()).addOnSuccessListener(semesters ->
                                SelectDialogHelper.show(requireContext(), getString(R.string.select_semester),
                                        semesters, Semester::getDisplayName, getString(R.string.no_semesters_exist),
                                        semester -> viewModel.listSubjectsForCourseAssign(semester.getId()).addOnSuccessListener(subjects ->
                                                SelectDialogHelper.show(requireContext(), getString(R.string.select_course),
                                                        subjects, s -> s.getCode() + " • " + s.getTitle(),
                                                        getString(R.string.no_assigned_subjects_yet), viewModel::assignCourse))))));
    }

    // ---- Account ----

    private void buildStatusActions(LinearLayout container, UserStatus status, boolean isSelf) {
        container.removeAllViews();
        if (isSelf) return;
        switch (status) {
            case PENDING:
                container.addView(actionButton(R.string.action_approve, false, v -> viewModel.approve()));
                container.addView(actionButton(R.string.action_reject, true, v -> viewModel.reject()));
                break;
            case APPROVED:
                container.addView(actionButton(R.string.action_suspend, true, v -> viewModel.suspendUser()));
                break;
            case SUSPENDED:
                container.addView(actionButton(R.string.action_activate, false, v -> viewModel.activate()));
                break;
            case REJECTED:
                break;
        }
    }

    private MaterialButton actionButton(int textRes, boolean outlined, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(requireContext(), null,
                outlined ? com.google.android.material.R.attr.materialButtonOutlinedStyle : com.google.android.material.R.attr.materialButtonStyle);
        button.setText(textRes);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginEnd(12);
        button.setLayoutParams(params);
        return button;
    }

    private void showEditProfileDialog(User user) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);
        TextInputEditText editFullName = dialogView.findViewById(R.id.editFullName);
        TextInputEditText editFatherName = dialogView.findViewById(R.id.editFatherName);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editCnic = dialogView.findViewById(R.id.editCnic);
        View layoutEmployeeId = dialogView.findViewById(R.id.layoutEmployeeId);
        View layoutDesignation = dialogView.findViewById(R.id.layoutDesignation);
        View layoutRegistrationNumber = dialogView.findViewById(R.id.layoutRegistrationNumber);
        View layoutRollNumber = dialogView.findViewById(R.id.layoutRollNumber);
        TextInputEditText editEmployeeId = dialogView.findViewById(R.id.editEmployeeId);
        TextInputEditText editDesignation = dialogView.findViewById(R.id.editDesignation);
        TextInputEditText editRegistrationNumber = dialogView.findViewById(R.id.editRegistrationNumber);
        TextInputEditText editRollNumber = dialogView.findViewById(R.id.editRollNumber);
        TextView textError = dialogView.findViewById(R.id.textError);

        editFullName.setText(user.getFullName());
        editFatherName.setText(user.getFatherName());
        editPhone.setText(user.getPhone());
        editCnic.setText(user.getCnic());

        boolean isHodOrTeacher = user.getRole() == UserRole.HOD || user.getRole() == UserRole.TEACHER;
        boolean isStudent = user.getRole() == UserRole.STUDENT;
        if (isHodOrTeacher) {
            layoutEmployeeId.setVisibility(View.VISIBLE);
            layoutDesignation.setVisibility(View.VISIBLE);
            editEmployeeId.setText(user.getEmployeeId());
            editDesignation.setText(user.getDesignation());
        } else if (isStudent) {
            layoutRegistrationNumber.setVisibility(View.VISIBLE);
            layoutRollNumber.setVisibility(View.VISIBLE);
            editRegistrationNumber.setText(user.getRegistrationNumber());
            editRollNumber.setText(user.getRollNumber());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_profile)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String fullName = textOf(editFullName);
            if (fullName.isBlank()) {
                textError.setVisibility(View.VISIBLE);
                textError.setText(R.string.full_name_required);
                return;
            }
            viewModel.updateProfile(fullName, textOf(editFatherName), textOf(editPhone), textOf(editCnic));
            if (isHodOrTeacher) {
                viewModel.updateIdentifiers(nullIfBlank(textOf(editEmployeeId)), null, null, nullIfBlank(textOf(editDesignation)));
            } else if (isStudent) {
                viewModel.updateIdentifiers(null, nullIfBlank(textOf(editRegistrationNumber)), nullIfBlank(textOf(editRollNumber)), null);
            }
            dialog.dismiss();
        }));
        dialog.show();
    }

    private static String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @Nullable
    private static String nullIfBlank(String value) {
        return value.isBlank() ? null : value;
    }

    private static void setLabel(View root, int rowId, int labelRes) {
        ((TextView) root.findViewById(rowId).findViewById(R.id.textLabel)).setText(labelRes);
    }

    private static void setValue(View root, int rowId, String value) {
        ((TextView) root.findViewById(rowId).findViewById(R.id.textValue)).setText(value);
    }

    @Nullable
    private static Department findById(List<Department> departments, @Nullable String id) {
        if (id == null) return null;
        for (Department department : departments) {
            if (department.getId().equals(id)) return department;
        }
        return null;
    }

    @Nullable
    private static Semester findSemesterById(List<Semester> semesters, @Nullable String id) {
        if (id == null) return null;
        for (Semester semester : semesters) {
            if (semester.getId().equals(id)) return semester;
        }
        return null;
    }

    @Nullable
    private static Session findSessionById(List<Session> sessions, @Nullable String id) {
        if (id == null) return null;
        for (Session session : sessions) {
            if (session.getId().equals(id)) return session;
        }
        return null;
    }
}
