package com.example.uos_lms.feature.admin.university.presentation.session;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SessionSemesterListFragment extends Fragment {

    private SessionSemesterListViewModel viewModel;
    private SimpleListAdapter<Semester> adapter;
    private RefreshUx.Binding refreshBinding;

    public SessionSemesterListFragment() {
        super(R.layout.fragment_admin_session_semester_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_session_semester_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SessionSemesterListViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.semesters_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_semesters_tap_add);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SimpleListAdapter<>(R.layout.item_semester_row, (itemView, semester, position) -> {
            ((TextView) itemView.findViewById(R.id.textLabel)).setText(semester.getDisplayName());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.role_admin_start);
            itemView.findViewById(R.id.textLabel).setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("departmentId", viewModel.getDepartmentId());
                args.putString("semesterId", semester.getId());
                args.putString("sessionId", viewModel.getSessionId());
                NavHostFragment.findNavController(this).navigate(R.id.semesterSubjectsFragment, args);
            });
            itemView.findViewById(R.id.buttonDelete).setOnClickListener(v ->
                    ConfirmDialogHelper.show(requireContext(),
                            getString(R.string.delete_semester_title_format, semester.getDisplayName()),
                            getString(R.string.delete_semester_message), getString(R.string.delete),
                            () -> viewModel.deleteSemester(semester.getId())));
        });
        recyclerList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v -> showAddSemesterDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            String title = state.getDepartmentName().isBlank() && state.getSessionLabel().isBlank()
                    ? getString(R.string.semesters_fallback_title)
                    : state.getDepartmentName() + " • " + state.getSessionLabel();
            ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);

            boolean hasSemesters = !state.getSemesters().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasSemesters ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(!state.isLoading() && hasSemesters ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSemesters());
            refreshBinding.setRefreshing(state.isRefreshing());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void showAddSemesterDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_semester, null);
        TextInputEditText editNumber = dialogView.findViewById(R.id.editNumber);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_semester_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String numberText = editNumber.getText() == null ? "" : editNumber.getText().toString().trim();
            Integer number = null;
            try {
                number = Integer.parseInt(numberText);
            } catch (NumberFormatException ignored) {
                // handled below via null check
            }
            if (number == null || number <= 0) {
                textError.setText(R.string.semester_number_invalid);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            textError.setVisibility(View.GONE);
            progressSaving.setVisibility(View.VISIBLE);
            editNumber.setEnabled(false);

            viewModel.addSemester(number).addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    dialog.dismiss();
                } else {
                    progressSaving.setVisibility(View.GONE);
                    editNumber.setEnabled(true);
                    textError.setText(result.getException() != null ? result.getException().getMessage() : null);
                    textError.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
