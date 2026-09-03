package com.example.uos_lms.feature.admin.presentation.results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.ui.AccentColors;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminResultsDepartmentListFragment extends Fragment {

    private AdminResultsDepartmentListViewModel viewModel;

    public AdminResultsDepartmentListFragment() {
        super(R.layout.fragment_admin_results_department_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_results_department_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminResultsDepartmentListViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.select_department);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.load());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_departments_exist);
        View progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Department> adapter = new SimpleListAdapter<>(R.layout.item_simple_nav_row, (itemView, department, position) -> {
            ((TextView) itemView.findViewById(R.id.textTitle)).setText(department.getName());
            TextView subtitle = itemView.findViewById(R.id.textSubtitle);
            if (department.getCode() != null && !department.getCode().isBlank()) {
                subtitle.setVisibility(View.VISIBLE);
                subtitle.setText(department.getCode());
            } else {
                subtitle.setVisibility(View.GONE);
            }
            AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_admin_start);

            itemView.setOnClickListener(v -> {
                Bundle navArgs = new Bundle();
                navArgs.putString("departmentId", department.getId());
                navArgs.putString("departmentName", department.getName());
                NavHostFragment.findNavController(this).navigate(R.id.adminResultsSessionListFragment, navArgs);
            });
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasDepartments = !state.getDepartments().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasDepartments ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(hasDepartments ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getDepartments());
            refreshBinding.setRefreshing(state.isLoading());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
