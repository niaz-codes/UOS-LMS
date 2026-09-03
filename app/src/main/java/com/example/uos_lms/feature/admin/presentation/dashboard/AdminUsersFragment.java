package com.example.uos_lms.feature.admin.presentation.dashboard;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.session.CachedSession;
import com.example.uos_lms.core.session.SessionManager;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.example.uos_lms.core.ui.StatusChipHelper;
import com.example.uos_lms.core.ui.UserAvatarHelper;
import com.example.uos_lms.core.ui.UserQuickActionsMenuHelper;
import com.example.uos_lms.feature.admin.domain.model.UserFilter;
import com.example.uos_lms.feature.admin.presentation.usermanagement.common.RoleTreeController;
import com.example.uos_lms.feature.admin.presentation.usermanagement.hod.AdminHodTreeViewModel;
import com.example.uos_lms.feature.admin.presentation.usermanagement.teacher.AdminTeacherTreeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminUsersFragment extends Fragment {

    @Inject
    SessionManager sessionManager;

    private AdminUserListViewModel allUsersViewModel;

    public AdminUsersFragment() {
        super(R.layout.fragment_admin_users);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.nav_users);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        FrameLayout tabContent = view.findViewById(R.id.tabContent);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_hod));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_teacher));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_student));

        View allUsersContent = LayoutInflater.from(requireContext()).inflate(R.layout.content_all_users, tabContent, false);
        View hodContent = LayoutInflater.from(requireContext()).inflate(R.layout.content_role_tree, tabContent, false);
        View teacherContent = LayoutInflater.from(requireContext()).inflate(R.layout.content_role_tree, tabContent, false);
        View[] tabViews = {allUsersContent, hodContent, teacherContent, null};

        // Optional deep-link from the Dashboard's User Management / Pending Approvals cards -
        // absent (the normal bottom-nav entry) it's just "land on All, no filter", same as before.
        Bundle args = getArguments();
        int initialTab = args != null ? args.getInt("initialTab", 0) : 0;
        String initialFilter = args != null ? args.getString("initialFilter") : null;

        bindAllUsersTab(allUsersContent, initialFilter);

        AdminHodTreeViewModel hodViewModel = new ViewModelProvider(this).get(AdminHodTreeViewModel.class);
        new RoleTreeController(this, hodViewModel, "HOD", getString(R.string.search_hods_hint), false,
                this::openUserDetail).bind(hodContent, getViewLifecycleOwner());

        AdminTeacherTreeViewModel teacherViewModel = new ViewModelProvider(this).get(AdminTeacherTreeViewModel.class);
        new RoleTreeController(this, teacherViewModel, "Teacher", getString(R.string.search_teachers_hint), true,
                this::openUserDetail).bind(teacherContent, getViewLifecycleOwner());

        tabContent.addView(allUsersContent);
        int startTab = initialTab >= 0 && initialTab <= 2 ? initialTab : 0;
        showTab(tabContent, tabViews, startTab);
        tabLayout.selectTab(tabLayout.getTabAt(startTab));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 3) {
                    NavHostFragment.findNavController(AdminUsersFragment.this).navigate(R.id.adminStudentTreeFragment);
                    tabLayout.selectTab(tabLayout.getTabAt(0));
                    return;
                }
                showTab(tabContent, tabViews, tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Users is no longer one of the 5 bottom-nav tabs (Home/Result/Departments/Reports/
        // Profile) - this screen is now reached only via the Home dashboard's User Management
        // cards, so nothing here should show as "selected".
        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.adminHomeFragment, false);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.adminResultsDepartmentListFragment);
            else if (id == R.id.navDepartments) NavHostFragment.findNavController(this).navigate(R.id.departmentListFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.adminReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });
    }

    private void showTab(FrameLayout container, View[] tabViews, int index) {
        container.removeAllViews();
        View selected = tabViews[index];
        if (selected != null) container.addView(selected);
    }

    private void openUserDetail(String uid) {
        Bundle args = new Bundle();
        args.putString("uid", uid);
        NavHostFragment.findNavController(this).navigate(R.id.adminUserDetailFragment, args);
    }

    private void bindAllUsersTab(View content, @Nullable String initialFilterName) {
        allUsersViewModel = new ViewModelProvider(this).get(AdminUserListViewModel.class);

        TextInputEditText editSearch = content.findViewById(R.id.editSearch);
        LinearLayout filterChipGroup = content.findViewById(R.id.filterChipGroup);
        View emptyState = content.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_users_found);
        RecyclerView recyclerList = content.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SwipeRefreshLayout swipeRefresh = content.findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(
                R.color.role_admin_start, R.color.role_hod_start, R.color.role_teacher_start, R.color.role_student_start);
        swipeRefresh.setOnRefreshListener(() -> allUsersViewModel.refresh());

        UserFilter initialFilter = UserFilter.ALL;
        if (initialFilterName != null) {
            for (UserFilter filter : UserFilter.values()) {
                if (filter.name().equals(initialFilterName)) initialFilter = filter;
            }
        }

        for (UserFilter filter : UserFilter.values()) {
            Chip chip = new Chip(requireContext());
            chip.setText(filter.getLabel());
            chip.setCheckable(true);
            chip.setChecked(filter == initialFilter);
            chip.setOnClickListener(v -> allUsersViewModel.onFilterChange(filter));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);
            filterChipGroup.addView(chip);
        }
        if (initialFilter != UserFilter.ALL) allUsersViewModel.onFilterChange(initialFilter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                allUsersViewModel.onSearchQueryChange(s.toString());
            }
        });

        SimpleListAdapter<User> adapter = new SimpleListAdapter<>(R.layout.item_user_row, (itemView, user, position) -> {
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), AccentColors.colorForRole(user.getRole()));
            UserAvatarHelper.bind(itemView.findViewById(R.id.imageAvatar), user.getProfilePhotoUrl());
            ((TextView) itemView.findViewById(R.id.textName)).setText(user.getFullName());
            ((TextView) itemView.findViewById(R.id.textEmail)).setText(user.getEmail());
            TextView extra1 = itemView.findViewById(R.id.textExtra1);
            extra1.setVisibility(View.VISIBLE);
            extra1.setText(user.getRole().name());
            StatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), user.getStatus());

            itemView.findViewById(R.id.clickableArea).setOnClickListener(v -> openUserDetail(user.getUid()));
            CachedSession session = sessionManager.getCachedSession().getValue();
            boolean isSelf = session != null && session.getUid().equals(user.getUid());
            UserQuickActionsMenuHelper.attach(itemView.findViewById(R.id.buttonMore), user, isSelf, new UserQuickActionsMenuHelper.Actions() {
                @Override
                public void onView() {
                    openUserDetail(user.getUid());
                }

                @Override
                public void onApprove() {
                    allUsersViewModel.approve(user.getUid());
                }

                @Override
                public void onReject() {
                    allUsersViewModel.reject(user.getUid());
                }

                @Override
                public void onSuspend() {
                    allUsersViewModel.suspendUser(user.getUid());
                }

                @Override
                public void onActivate() {
                    allUsersViewModel.activate(user.getUid());
                }

                @Override
                public void onDeleteRequested() {
                    ConfirmDialogHelper.show(requireContext(),
                            getString(R.string.delete_user_title),
                            getString(R.string.delete_user_message, user.getFullName()),
                            getString(R.string.delete),
                            () -> allUsersViewModel.delete(user.getUid()));
                }

                @Override
                public void onResetPassword() {
                    allUsersViewModel.resetPassword(user.getEmail());
                }
            });
        });
        recyclerList.setAdapter(adapter);

        allUsersViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasUsers = !state.getFilteredUsers().isEmpty();
            emptyState.setVisibility(hasUsers || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getFilteredUsers());
            if (swipeRefresh.isRefreshing() != state.isRefreshing()) swipeRefresh.setRefreshing(state.isRefreshing());

            if (state.getErrorMessage() != null) {
                Snackbar.make(content, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                allUsersViewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(content, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                allUsersViewModel.consumeMessages();
            }
        });
    }
}
