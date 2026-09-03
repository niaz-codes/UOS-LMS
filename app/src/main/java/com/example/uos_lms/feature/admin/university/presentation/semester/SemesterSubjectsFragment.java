package com.example.uos_lms.feature.admin.university.presentation.semester;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.example.uos_lms.core.ui.UserQuickActionsMenuHelper;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SemesterSubjectsFragment extends Fragment {

    private SemesterSubjectsViewModel viewModel;
    private RefreshUx.Binding refreshBinding;

    public SemesterSubjectsFragment() {
        super(R.layout.fragment_admin_semester_subjects);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_semester_subjects, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SemesterSubjectsViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.semester_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        view.findViewById(R.id.buttonSort).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            for (UserSortOption option : UserSortOption.values()) {
                popup.getMenu().add(option.getLabel()).setOnMenuItemClickListener(item -> {
                    viewModel.onStudentSortChange(option);
                    return true;
                });
            }
            popup.show();
        });

        view.findViewById(R.id.buttonLoadMoreStudents).setOnClickListener(v -> viewModel.onStudentLoadMore());
        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showSubjectFormDialog(null));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, SemesterSubjectsUiState state) {
        View toolbar = view.findViewById(R.id.toolbar);
        String title = state.getSemester() != null ? state.getSemester().getDisplayName() : getString(R.string.semester_fallback_title);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);

        LinearLayout filterChipGroup = view.findViewById(R.id.filterChipGroup);
        filterChipGroup.removeAllViews();
        for (UserFilter filter : UserFilter.values()) {
            Chip chip = new Chip(requireContext());
            chip.setText(filter.getLabel());
            chip.setCheckable(true);
            chip.setChecked(state.getStudentFilter() == filter);
            chip.setOnClickListener(v -> viewModel.onStudentFilterChange(filter));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);
            filterChipGroup.addView(chip);
        }

        TextView textStudentsStatus = view.findViewById(R.id.textStudentsStatus);
        if (state.isLoadingStudents()) {
            textStudentsStatus.setVisibility(View.VISIBLE);
            textStudentsStatus.setText(R.string.loading_students);
        } else if (state.getFilteredStudents().isEmpty()) {
            textStudentsStatus.setVisibility(View.VISIBLE);
            textStudentsStatus.setText(R.string.no_students_found_scope);
        } else {
            textStudentsStatus.setVisibility(View.GONE);
        }

        LinearLayout studentsContainer = view.findViewById(R.id.studentsContainer);
        studentsContainer.removeAllViews();
        for (User student : state.getVisibleStudents()) {
            studentsContainer.addView(buildStudentRow(studentsContainer, student, state));
        }

        MaterialButton buttonLoadMoreStudents = view.findViewById(R.id.buttonLoadMoreStudents);
        if (state.isHasMoreStudents()) {
            buttonLoadMoreStudents.setVisibility(View.VISIBLE);
            buttonLoadMoreStudents.setText(getString(R.string.load_more_format,
                    state.getFilteredStudents().size() - state.getVisibleStudentCount()));
        } else {
            buttonLoadMoreStudents.setVisibility(View.GONE);
        }

        view.findViewById(R.id.textNoSubjects).setVisibility(
                state.getSubjects().isEmpty() && !state.isLoading() ? View.VISIBLE : View.GONE);

        LinearLayout subjectsContainer = view.findViewById(R.id.subjectsContainer);
        subjectsContainer.removeAllViews();
        for (Subject subject : state.getSubjects()) {
            subjectsContainer.addView(buildSubjectRow(subjectsContainer, subject, state));
        }

        refreshBinding.setRefreshing(state.isRefreshing());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeActionMessage();
        }
    }

    private View buildStudentRow(LinearLayout parent, User student, SemesterSubjectsUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_card, parent, false);
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), student.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(student.getFullName());
        ((TextView) row.findViewById(R.id.textRegistrationNumber)).setText(
                getString(R.string.registration_number) + ": " + (student.getRegistrationNumber() != null ? student.getRegistrationNumber() : getString(R.string.not_assigned)));
        ((TextView) row.findViewById(R.id.textRollNumber)).setText(
                getString(R.string.roll_number) + ": " + (student.getRollNumber() != null ? student.getRollNumber() : getString(R.string.not_assigned)));
        String semesterLabel = state.getSemester() != null ? state.getSemester().getDisplayName() : "";
        ((TextView) row.findViewById(R.id.textScope)).setText(
                state.getDepartmentName() + " • " + state.getSessionLabel() + " • " + semesterLabel);
        ((TextView) row.findViewById(R.id.textEmail)).setText(student.getEmail());
        com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar),
                StatusChipHelper.bind(row.findViewById(R.id.textStatusChip), student.getStatus()));

        row.findViewById(R.id.clickableArea).setOnClickListener(v -> openUserDetail(student.getUid()));
        UserQuickActionsMenuHelper.attach(row.findViewById(R.id.buttonMore), student, false, new UserQuickActionsMenuHelper.Actions() {
            @Override
            public void onView() {
                openUserDetail(student.getUid());
            }

            @Override
            public void onApprove() {
                viewModel.approveStudent(student.getUid());
            }

            @Override
            public void onReject() {
                viewModel.rejectStudent(student.getUid());
            }

            @Override
            public void onSuspend() {
                viewModel.suspendStudent(student.getUid());
            }

            @Override
            public void onActivate() {
                viewModel.activateStudent(student.getUid());
            }

            @Override
            public void onDeleteRequested() {
                ConfirmDialogHelper.show(requireContext(), getString(R.string.delete_user_title),
                        getString(R.string.delete_user_message, student.getFullName()), getString(R.string.delete),
                        () -> viewModel.deleteStudent(student.getUid()));
            }

            @Override
            public void onResetPassword() {
                viewModel.resetStudentPassword(student.getEmail());
            }
        });

        return row;
    }

    private void openUserDetail(String uid) {
        Bundle args = new Bundle();
        args.putString("uid", uid);
        NavHostFragment.findNavController(this).navigate(R.id.adminUserDetailFragment, args);
    }

    private View buildSubjectRow(LinearLayout parent, Subject subject, SemesterSubjectsUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_subject_row, parent, false);
        ((TextView) row.findViewById(R.id.textTitle)).setText(subject.getTitle());
        ((TextView) row.findViewById(R.id.textCodeCredits)).setText(
                subject.getCode() + " • " + subject.getCreditHours() + " credit hours");

        TextView textTeacher = row.findViewById(R.id.textTeacher);
        if (subject.getTeacherName() != null) {
            textTeacher.setText(getString(R.string.teacher_assigned_format, subject.getTeacherName()));
            textTeacher.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.indigo_primary));
        } else {
            textTeacher.setText(R.string.teacher_not_assigned);
            textTeacher.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error_color));
        }
        com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar), R.color.role_admin_start);

        MaterialButton buttonAssignTeacher = row.findViewById(R.id.buttonAssignTeacher);
        buttonAssignTeacher.setText(subject.getTeacherUid() == null ? R.string.assign_teacher : R.string.change_teacher);
        buttonAssignTeacher.setOnClickListener(v ->
                SelectDialogHelper.show(requireContext(), getString(R.string.assign_teacher_title),
                        state.getTeachers(), User::getFullName, getString(R.string.no_approved_teachers),
                        teacher -> viewModel.assignTeacher(subject.getId(), teacher)));

        View buttonUnassignTeacher = row.findViewById(R.id.buttonUnassignTeacher);
        buttonUnassignTeacher.setVisibility(subject.getTeacherUid() != null ? View.VISIBLE : View.GONE);
        buttonUnassignTeacher.setOnClickListener(v -> viewModel.unassignTeacher(subject.getId()));

        row.findViewById(R.id.buttonEdit).setOnClickListener(v -> showSubjectFormDialog(subject));
        row.findViewById(R.id.buttonDelete).setOnClickListener(v ->
                ConfirmDialogHelper.show(requireContext(), getString(R.string.delete_subject_title),
                        getString(R.string.delete_subject_message_format, subject.getTitle(), subject.getCode()),
                        getString(R.string.delete), () -> viewModel.deleteSubject(subject.getId())));

        return row;
    }

    private void showSubjectFormDialog(@Nullable Subject initial) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_subject_form, null);
        TextInputEditText editCode = dialogView.findViewById(R.id.editCode);
        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editCreditHours = dialogView.findViewById(R.id.editCreditHours);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        if (initial != null) {
            editCode.setText(initial.getCode());
            editTitle.setText(initial.getTitle());
            editCreditHours.setText(String.valueOf(initial.getCreditHours()));
        }

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(initial == null ? R.string.add_subject_title : R.string.edit_subject_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String code = editCode.getText() == null ? "" : editCode.getText().toString().trim().toUpperCase();
            String title = editTitle.getText() == null ? "" : editTitle.getText().toString().trim();
            String creditHoursText = editCreditHours.getText() == null ? "" : editCreditHours.getText().toString().trim();

            if (code.isEmpty() || title.isEmpty()) {
                textError.setText(R.string.code_title_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            Integer creditHours = null;
            try {
                creditHours = Integer.parseInt(creditHoursText);
            } catch (NumberFormatException ignored) {
                // handled below via null check
            }
            if (creditHours == null || creditHours <= 0) {
                textError.setText(R.string.credit_hours_invalid);
                textError.setVisibility(View.VISIBLE);
                return;
            }

            textError.setVisibility(View.GONE);
            progressSaving.setVisibility(View.VISIBLE);
            editCode.setEnabled(false);
            editTitle.setEnabled(false);
            editCreditHours.setEnabled(false);

            var task = initial == null
                    ? viewModel.createSubject(code, title, creditHours)
                    : viewModel.updateSubject(initial.getId(), code, title, creditHours);
            int finalCreditHours = creditHours;
            task.addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    dialog.dismiss();
                } else {
                    progressSaving.setVisibility(View.GONE);
                    editCode.setEnabled(true);
                    editTitle.setEnabled(true);
                    editCreditHours.setEnabled(true);
                    textError.setText(result.getException() != null ? result.getException().getMessage() : null);
                    textError.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
