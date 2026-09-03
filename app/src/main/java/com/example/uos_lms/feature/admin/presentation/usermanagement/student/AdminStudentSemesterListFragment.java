package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.example.uos_lms.core.ui.UserQuickActionsMenuHelper;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.domain.model.UserSortOption;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.function.Consumer;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminStudentSemesterListFragment extends Fragment {

    private AdminStudentSemesterListViewModel viewModel;
    private boolean suppressSemesterSearchWatcher;

    public AdminStudentSemesterListFragment() {
        super(R.layout.fragment_admin_student_semester_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_student_semester_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminStudentSemesterListViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.student_semester_list_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        TextInputEditText editSemesterSearch = view.findViewById(R.id.editSemesterSearch);
        editSemesterSearch.addTextChangedListener(watcher(text -> {
            if (!suppressSemesterSearchWatcher) viewModel.onSemesterSearchChange(text);
        }));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semesters_in_department);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, toolbar, editSemesterSearch, emptyState, state));
    }

    private void render(View view, View toolbar, TextInputEditText editSemesterSearch, View emptyState, AdminStudentSemesterListUiState state) {
        String title = state.getDepartmentName().isBlank() && state.getSessionLabel().isBlank()
                ? getString(R.string.student_semester_list_fallback_title)
                : state.getDepartmentName() + " • " + state.getSessionLabel();
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);

        suppressSemesterSearchWatcher = true;
        String current = editSemesterSearch.getText() == null ? "" : editSemesterSearch.getText().toString();
        if (!current.equals(state.getSemesterSearchQuery())) {
            editSemesterSearch.setText(state.getSemesterSearchQuery());
        }
        suppressSemesterSearchWatcher = false;

        boolean hasNodes = !state.getFilteredSemesters().isEmpty();
        emptyState.setVisibility(hasNodes || state.isLoading() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.scrollNodes).setVisibility(hasNodes ? View.VISIBLE : View.GONE);

        LinearLayout nodesContainer = view.findViewById(R.id.nodesContainer);
        nodesContainer.removeAllViews();
        for (SemesterStudentsNode node : state.getFilteredSemesters()) {
            nodesContainer.addView(buildNodeView(nodesContainer, node, state));
        }

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessages();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeMessages();
        }
    }

    private View buildNodeView(LinearLayout parent, SemesterStudentsNode node, AdminStudentSemesterListUiState state) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_department_tree_node, parent, false);

        ((TextView) view.findViewById(R.id.textTitle)).setText(node.getSemester().getDisplayName());
        view.findViewById(R.id.textSubtitle).setVisibility(View.GONE);
        TextView countBadge = view.findViewById(R.id.textCountBadge);
        countBadge.setText(node.getTotalCount() != null ? String.valueOf(node.getTotalCount()) : "…");
        ImageView chevron = view.findViewById(R.id.imageChevron);
        chevron.setRotation(node.isExpanded() ? 180f : 0f);
        com.example.uos_lms.core.ui.AccentColors.applyBar(view.findViewById(R.id.accentBar), R.color.role_admin_start);

        view.findViewById(R.id.headerCard).setOnClickListener(v -> viewModel.toggleSemester(node.getSemester().getId()));

        View contentSection = view.findViewById(R.id.contentSection);
        contentSection.setVisibility(node.isExpanded() ? View.VISIBLE : View.GONE);

        if (node.isExpanded()) {
            bindExpandedContent(view, node, state);
        }

        return view;
    }

    private void bindExpandedContent(View view, SemesterStudentsNode node, AdminStudentSemesterListUiState state) {
        String semesterId = node.getSemester().getId();

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.setText(node.getSearchQuery());
        editSearch.setHint(R.string.search_students_scoped_hint);
        editSearch.addTextChangedListener(watcher(text -> viewModel.onStudentSearchChange(semesterId, text)));

        LinearLayout filterChipGroup = view.findViewById(R.id.filterChipGroup);
        filterChipGroup.removeAllViews();
        for (UserFilter filter : UserFilter.values()) {
            Chip chip = new Chip(requireContext());
            chip.setText(filter.getLabel());
            chip.setCheckable(true);
            chip.setChecked(node.getFilter() == filter);
            chip.setOnClickListener(v -> viewModel.onStudentFilterChange(semesterId, filter));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);
            filterChipGroup.addView(chip);
        }

        view.findViewById(R.id.buttonSort).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            for (UserSortOption option : UserSortOption.values()) {
                popup.getMenu().add(option.getLabel()).setOnMenuItemClickListener(item -> {
                    viewModel.onStudentSortChange(semesterId, option);
                    return true;
                });
            }
            popup.show();
        });

        TextView statusMessage = view.findViewById(R.id.textStatusMessage);
        LinearLayout userListContainer = view.findViewById(R.id.userListContainer);
        userListContainer.removeAllViews();

        if (node.isLoadingStudents()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.loading_students);
        } else if (node.getFilteredStudents().isEmpty()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.no_students_found_scope);
        } else {
            statusMessage.setVisibility(View.GONE);
            for (User student : node.getVisibleStudents()) {
                userListContainer.addView(buildStudentRow(userListContainer, student, node, state));
            }
        }

        com.google.android.material.button.MaterialButton loadMore = view.findViewById(R.id.buttonLoadMore);
        if (node.isHasMore()) {
            loadMore.setVisibility(View.VISIBLE);
            loadMore.setText(getString(R.string.load_more_format, node.getFilteredStudents().size() - node.getVisibleCount()));
            loadMore.setOnClickListener(v -> viewModel.onStudentLoadMore(semesterId));
        } else {
            loadMore.setVisibility(View.GONE);
        }
    }

    private View buildStudentRow(LinearLayout parent, User student, SemesterStudentsNode node, AdminStudentSemesterListUiState state) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_card, parent, false);
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), student.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(student.getFullName());
        ((TextView) row.findViewById(R.id.textRegistrationNumber)).setText(
                getString(R.string.registration_number) + ": " + (student.getRegistrationNumber() != null ? student.getRegistrationNumber() : getString(R.string.not_assigned)));
        ((TextView) row.findViewById(R.id.textRollNumber)).setText(
                getString(R.string.roll_number) + ": " + (student.getRollNumber() != null ? student.getRollNumber() : getString(R.string.not_assigned)));
        ((TextView) row.findViewById(R.id.textScope)).setText(
                state.getDepartmentName() + " • " + state.getSessionLabel() + " • " + node.getSemester().getDisplayName());
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
                viewModel.approve(student.getUid());
            }

            @Override
            public void onReject() {
                viewModel.reject(student.getUid());
            }

            @Override
            public void onSuspend() {
                viewModel.suspend(student.getUid());
            }

            @Override
            public void onActivate() {
                viewModel.activate(student.getUid());
            }

            @Override
            public void onDeleteRequested() {
                ConfirmDialogHelper.show(requireContext(), getString(R.string.delete_user_title),
                        getString(R.string.delete_user_message, student.getFullName()), getString(R.string.delete),
                        () -> viewModel.delete(student.getUid(), node.getSemester().getId()));
            }

            @Override
            public void onResetPassword() {
                viewModel.resetPassword(student.getEmail());
            }
        });

        return row;
    }

    private void openUserDetail(String uid) {
        Bundle args = new Bundle();
        args.putString("uid", uid);
        NavHostFragment.findNavController(this).navigate(R.id.adminUserDetailFragment, args);
    }

    private TextWatcher watcher(Consumer<String> onChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChanged.accept(s.toString());
            }
        };
    }
}
