package com.example.uos_lms.feature.admin.university.presentation.department;

import android.app.Dialog;
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
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DepartmentListFragment extends Fragment {

    private DepartmentListViewModel viewModel;

    public DepartmentListFragment() {
        super(R.layout.fragment_admin_department_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_department_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DepartmentListViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.departments_title);
        toolbar.findViewById(R.id.buttonBack).setVisibility(View.GONE);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RefreshUx.Binding refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_departments_tap_add);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<Department> adapter = new SimpleListAdapter<>(R.layout.item_department, (itemView, department, position) -> {
            ((TextView) itemView.findViewById(R.id.textName)).setText(department.getName());
            ((TextView) itemView.findViewById(R.id.textCode)).setText(department.getCode());
            TextView description = itemView.findViewById(R.id.textDescription);
            if (department.getDescription() != null && !department.getDescription().isBlank()) {
                description.setVisibility(View.VISIBLE);
                description.setText(department.getDescription());
            } else {
                description.setVisibility(View.GONE);
            }
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_admin_start);
            itemView.findViewById(R.id.clickableArea).setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("departmentId", department.getId());
                NavHostFragment.findNavController(this).navigate(R.id.departmentDetailFragment, args);
            });
            itemView.findViewById(R.id.buttonEdit).setOnClickListener(v -> showFormDialog(department));
            itemView.findViewById(R.id.buttonDelete).setOnClickListener(v ->
                    ConfirmDialogHelper.show(requireContext(),
                            getString(R.string.delete_department_title),
                            getString(R.string.delete_department_message, department.getName()),
                            getString(R.string.delete),
                            () -> viewModel.deleteDepartment(department.getId())));
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showFormDialog(null));

        BottomNavigationView bottomNav = view.findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.navDepartments);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navDepartments) return true;
            if (id == R.id.navHome) NavHostFragment.findNavController(this).popBackStack(R.id.adminHomeFragment, false);
            else if (id == R.id.navResult) NavHostFragment.findNavController(this).navigate(R.id.adminResultsDepartmentListFragment);
            else if (id == R.id.navReports) NavHostFragment.findNavController(this).navigate(R.id.adminReportsFragment);
            else if (id == R.id.navProfile) NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasDepartments = !state.getDepartments().isEmpty();
            emptyState.setVisibility(hasDepartments || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasDepartments ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getDepartments());
            refreshBinding.setRefreshing(state.isRefreshing());

            if (state.getListErrorMessage() != null) {
                Snackbar.make(view, state.getListErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearListError();
            }
        });
    }

    private void showFormDialog(@Nullable Department initial) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_department_form, null);
        TextInputLayout layoutName = dialogView.findViewById(R.id.layoutName);
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        TextInputLayout layoutCode = dialogView.findViewById(R.id.layoutCode);
        TextInputEditText editCode = dialogView.findViewById(R.id.editCode);
        TextInputEditText editDescription = dialogView.findViewById(R.id.editDescription);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        if (initial != null) {
            editName.setText(initial.getName());
            editCode.setText(initial.getCode());
            editDescription.setText(initial.getDescription());
        }

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(initial == null ? R.string.add_department_title : R.string.edit_department_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String name = editName.getText() == null ? "" : editName.getText().toString().trim();
            String code = editCode.getText() == null ? "" : editCode.getText().toString().trim().toUpperCase();
            String description = editDescription.getText() == null ? "" : editDescription.getText().toString().trim();
            if (name.isEmpty() || code.isEmpty()) {
                textError.setText(R.string.name_code_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            textError.setVisibility(View.GONE);
            progressSaving.setVisibility(View.VISIBLE);
            layoutName.setEnabled(false);
            layoutCode.setEnabled(false);

            var task = initial == null
                    ? viewModel.createDepartment(name, code, description)
                    : viewModel.updateDepartment(initial.getId(), name, code, description);
            task.addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    dialog.dismiss();
                } else {
                    progressSaving.setVisibility(View.GONE);
                    layoutName.setEnabled(true);
                    layoutCode.setEnabled(true);
                    textError.setText(result.getException() != null ? result.getException().getMessage() : null);
                    textError.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
