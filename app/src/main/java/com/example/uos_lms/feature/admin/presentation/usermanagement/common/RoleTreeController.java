package com.example.uos_lms.feature.admin.presentation.usermanagement.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.AccentColors;
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

/** Wires a `content_role_tree.xml` container to an [AdminRoleTreeViewModel] instance - shared
 * between the Admin "Users" screen's HOD and Teacher tabs, since both are identical
 * Department -> role-scoped-member expandable trees. Not a Fragment itself: it's bound
 * directly into a tab content view already inflated by the owning fragment. */
public class RoleTreeController {

    private final Fragment fragment;
    private final AdminRoleTreeViewModel viewModel;
    private final String memberLabel;
    private final String searchLabel;
    private final boolean showDepartmentInExtra;
    private final Consumer<String> onOpenUser;

    private boolean suppressDeptSearchWatcher;

    public RoleTreeController(
            Fragment fragment,
            AdminRoleTreeViewModel viewModel,
            String memberLabel,
            String searchLabel,
            boolean showDepartmentInExtra,
            Consumer<String> onOpenUser) {
        this.fragment = fragment;
        this.viewModel = viewModel;
        this.memberLabel = memberLabel;
        this.searchLabel = searchLabel;
        this.showDepartmentInExtra = showDepartmentInExtra;
        this.onOpenUser = onOpenUser;
    }

    public void bind(View root, LifecycleOwner lifecycleOwner) {
        TextInputEditText editDepartmentSearch = root.findViewById(R.id.editDepartmentSearch);
        LinearLayout statsRow = root.findViewById(R.id.statsRow);
        View emptyState = root.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_departments_found);
        SwipeRefreshLayout scrollNodes = root.findViewById(R.id.scrollNodes);
        LinearLayout nodesContainer = root.findViewById(R.id.nodesContainer);

        scrollNodes.setColorSchemeResources(
                R.color.role_admin_start, R.color.role_hod_start, R.color.role_teacher_start, R.color.role_student_start);
        scrollNodes.setOnRefreshListener(viewModel::refresh);

        editDepartmentSearch.addTextChangedListener(watcher(text -> {
            if (!suppressDeptSearchWatcher) viewModel.onDepartmentSearchChange(text);
        }));

        viewModel.getUiState().observe(lifecycleOwner, state -> {
            suppressDeptSearchWatcher = true;
            String current = editDepartmentSearch.getText() == null ? "" : editDepartmentSearch.getText().toString();
            if (!current.equals(state.getDepartmentSearchQuery())) {
                editDepartmentSearch.setText(state.getDepartmentSearchQuery());
            }
            suppressDeptSearchWatcher = false;

            statsRow.removeAllViews();
            addStatPill(statsRow, state.getNodes().size() + " Departments");
            addStatPill(statsRow, (state.getTotalCount() != null ? state.getTotalCount() : "…") + " " + memberLabel + "s");

            boolean hasNodes = !state.getFilteredNodes().isEmpty();
            emptyState.setVisibility(hasNodes || state.isLoadingDepartments() ? View.GONE : View.VISIBLE);
            scrollNodes.setVisibility(hasNodes ? View.VISIBLE : View.GONE);

            nodesContainer.removeAllViews();
            for (DepartmentUserNode node : state.getFilteredNodes()) {
                nodesContainer.addView(buildNodeView(nodesContainer, node));
            }

            if (scrollNodes.isRefreshing() != state.isRefreshing()) scrollNodes.setRefreshing(state.isRefreshing());

            if (state.getErrorMessage() != null) {
                Snackbar.make(root, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(root, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void addStatPill(LinearLayout container, String text) {
        TextView pill = new TextView(fragment.requireContext());
        pill.setText(text);
        pill.setTextSize(12f);
        pill.setBackgroundResource(R.drawable.bg_stat_pill);
        int hPad = (int) (12 * fragment.getResources().getDisplayMetrics().density);
        int vPad = (int) (6 * fragment.getResources().getDisplayMetrics().density);
        pill.setPadding(hPad, vPad, hPad, vPad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(hPad);
        pill.setLayoutParams(params);
        container.addView(pill);
    }

    private View buildNodeView(LinearLayout parent, DepartmentUserNode node) {
        View view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.item_department_tree_node, parent, false);
        Department department = node.getDepartment();

        ((TextView) view.findViewById(R.id.textTitle)).setText(department.getName());
        ((TextView) view.findViewById(R.id.textSubtitle)).setText(department.getCode());
        TextView countBadge = view.findViewById(R.id.textCountBadge);
        countBadge.setText(node.getTotalCount() != null ? String.valueOf(node.getTotalCount()) : "…");
        ImageView chevron = view.findViewById(R.id.imageChevron);
        chevron.setRotation(node.isExpanded() ? 180f : 0f);
        com.example.uos_lms.core.ui.AccentColors.applyBar(view.findViewById(R.id.accentBar), R.color.role_admin_start);

        view.findViewById(R.id.headerCard).setOnClickListener(v -> viewModel.toggleDepartment(department.getId()));

        View contentSection = view.findViewById(R.id.contentSection);
        contentSection.setVisibility(node.isExpanded() ? View.VISIBLE : View.GONE);

        if (node.isExpanded()) {
            bindExpandedContent(view, node);
        }

        return view;
    }

    private void bindExpandedContent(View view, DepartmentUserNode node) {
        String departmentId = node.getDepartment().getId();

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.setText(node.getSearchQuery());
        editSearch.setHint(searchLabel);
        editSearch.addTextChangedListener(watcher(text -> viewModel.onNodeSearchChange(departmentId, text)));

        LinearLayout filterChipGroup = view.findViewById(R.id.filterChipGroup);
        filterChipGroup.removeAllViews();
        for (UserFilter filter : UserFilter.values()) {
            Chip chip = new Chip(fragment.requireContext());
            chip.setText(filter.getLabel());
            chip.setCheckable(true);
            chip.setChecked(node.getFilter() == filter);
            chip.setOnClickListener(v -> viewModel.onNodeFilterChange(departmentId, filter));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);
            filterChipGroup.addView(chip);
        }

        view.findViewById(R.id.buttonSort).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(fragment.requireContext(), v);
            for (UserSortOption option : UserSortOption.values()) {
                popup.getMenu().add(option.getLabel()).setOnMenuItemClickListener(item -> {
                    viewModel.onNodeSortChange(departmentId, option);
                    return true;
                });
            }
            popup.show();
        });

        TextView statusMessage = view.findViewById(R.id.textStatusMessage);
        LinearLayout userListContainer = view.findViewById(R.id.userListContainer);
        userListContainer.removeAllViews();

        if (node.isLoadingUsers()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.loading_ellipsis);
        } else if (node.getFilteredUsers().isEmpty()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.no_users_in_department);
        } else {
            statusMessage.setVisibility(View.GONE);
            for (User user : node.getVisibleUsers()) {
                userListContainer.addView(buildUserRow(userListContainer, user, node.getDepartment().getId(), node.getDepartment().getName()));
            }
        }

        com.google.android.material.button.MaterialButton loadMore = view.findViewById(R.id.buttonLoadMore);
        if (node.isHasMore()) {
            loadMore.setVisibility(View.VISIBLE);
            loadMore.setText(fragment.getString(R.string.load_more_format,
                    node.getFilteredUsers().size() - node.getVisibleCount()));
            loadMore.setOnClickListener(v -> viewModel.onNodeLoadMore(departmentId));
        } else {
            loadMore.setVisibility(View.GONE);
        }
    }

    private View buildUserRow(LinearLayout parent, User user, String departmentId, String departmentName) {
        View row = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.item_user_row, parent, false);
        AccentColors.applyBar(row.findViewById(R.id.accentBar), AccentColors.colorForRole(user.getRole()));
        UserAvatarHelper.bind(row.findViewById(R.id.imageAvatar), user.getProfilePhotoUrl());
        ((TextView) row.findViewById(R.id.textName)).setText(user.getFullName());
        ((TextView) row.findViewById(R.id.textEmail)).setText(user.getEmail());

        TextView extra1 = row.findViewById(R.id.textExtra1);
        TextView extra2 = row.findViewById(R.id.textExtra2);
        if (showDepartmentInExtra) {
            extra1.setVisibility(View.VISIBLE);
            extra1.setText(fragment.getString(R.string.department_label_format, departmentName));
            extra2.setVisibility(View.VISIBLE);
            extra2.setText(fragment.getString(R.string.designation_label_format,
                    user.getDesignation() != null ? user.getDesignation() : fragment.getString(R.string.designation_not_assigned)));
        } else if (user.getDesignation() != null && !user.getDesignation().isBlank()) {
            extra1.setVisibility(View.VISIBLE);
            extra1.setText(user.getDesignation());
        }

        StatusChipHelper.bind(row.findViewById(R.id.textStatusChip), user.getStatus());

        row.findViewById(R.id.clickableArea).setOnClickListener(v -> onOpenUser.accept(user.getUid()));
        UserQuickActionsMenuHelper.attach(row.findViewById(R.id.buttonMore), user, false, new UserQuickActionsMenuHelper.Actions() {
            @Override
            public void onView() {
                onOpenUser.accept(user.getUid());
            }

            @Override
            public void onApprove() {
                viewModel.approve(user.getUid());
            }

            @Override
            public void onReject() {
                viewModel.reject(user.getUid());
            }

            @Override
            public void onSuspend() {
                viewModel.suspend(user.getUid());
            }

            @Override
            public void onActivate() {
                viewModel.activate(user.getUid());
            }

            @Override
            public void onDeleteRequested() {
                ConfirmDialogHelper.show(fragment.requireContext(),
                        fragment.getString(R.string.delete_user_title),
                        fragment.getString(R.string.delete_user_message, user.getFullName()),
                        fragment.getString(R.string.delete),
                        () -> viewModel.delete(user.getUid(), departmentId));
            }

            @Override
            public void onResetPassword() {
                viewModel.resetPassword(user.getEmail());
            }
        });

        return row;
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
