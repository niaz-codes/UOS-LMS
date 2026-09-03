package com.example.uos_lms.feature.admin.university.presentation.department;

import android.app.Dialog;
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
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.ui.ConfirmDialogHelper;
import com.example.uos_lms.core.ui.RefreshUx;
import com.example.uos_lms.core.ui.SelectDialogHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DepartmentDetailFragment extends Fragment {

    private DepartmentDetailViewModel viewModel;
    private SessionDeleteCheck lastShownDeleteCheck;
    private RefreshUx.Binding refreshBinding;

    public DepartmentDetailFragment() {
        super(R.layout.fragment_admin_department_detail);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_department_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DepartmentDetailViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.department_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.contentContainer);
        refreshBinding = RefreshUx.bindToolbarIcon(
                toolbar.findViewById(R.id.toolbarActionSlot), swipeRefresh, () -> viewModel.refresh());

        view.findViewById(R.id.buttonEditDepartment).setOnClickListener(v -> {
            Department department = currentDepartment();
            if (department != null) showDepartmentFormDialog(department);
        });
        view.findViewById(R.id.buttonDeleteDepartment).setOnClickListener(v -> {
            Department department = currentDepartment();
            if (department == null) return;
            ConfirmDialogHelper.show(requireContext(), getString(R.string.delete_department_title),
                    getString(R.string.delete_department_message, department.getName()), getString(R.string.delete),
                    () -> viewModel.deleteDepartment(() -> NavHostFragment.findNavController(this).popBackStack()));
        });
        view.findViewById(R.id.buttonAddSession).setOnClickListener(v -> showSessionFormDialog(null));

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private Department currentDepartment() {
        return viewModel.getUiState().getValue().getDepartment();
    }

    private void render(View view, DepartmentDetailUiState state) {
        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        Department department = state.getDepartment();

        boolean showLoading = state.isLoading() || department == null;
        progressLoading.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(showLoading ? View.GONE : View.VISIBLE);
        if (department == null) return;

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(department.getName());

        ((TextView) view.findViewById(R.id.textCode)).setText(department.getCode());
        TextView textDescription = view.findViewById(R.id.textDescription);
        if (department.getDescription() != null && !department.getDescription().isBlank()) {
            textDescription.setVisibility(View.VISIBLE);
            textDescription.setText(department.getDescription());
        } else {
            textDescription.setVisibility(View.GONE);
        }

        view.findViewById(R.id.textNoSessions).setVisibility(state.getSessions().isEmpty() ? View.VISIBLE : View.GONE);

        LinearLayout sessionsContainer = view.findViewById(R.id.sessionsContainer);
        sessionsContainer.removeAllViews();
        for (Session session : state.getSessions()) {
            sessionsContainer.addView(buildSessionRow(sessionsContainer, session, state.getUpdatingSessionIds().contains(session.getId())));
        }

        refreshBinding.setRefreshing(state.isRefreshing());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.clearError();
        }

        if (state.getSessionDeleteCheck() != null) {
            if (state.getSessionDeleteCheck() != lastShownDeleteCheck) {
                lastShownDeleteCheck = state.getSessionDeleteCheck();
                showSessionDeleteDialog(state.getSessionDeleteCheck(), state.getSessions());
            }
        } else {
            lastShownDeleteCheck = null;
        }
    }

    private View buildSessionRow(LinearLayout parent, Session session, boolean isUpdating) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_session_row, parent, false);
        ((TextView) row.findViewById(R.id.textLabel)).setText(session.getLabel());
        TextView textStatus = row.findViewById(R.id.textStatus);
        int statusColorRes = session.isActive() ? R.color.status_success : R.color.on_surface_variant_color;
        com.example.uos_lms.core.ui.AccentColors.applyPill(textStatus, statusColorRes,
                getString(session.isActive() ? R.string.active_status : R.string.inactive_status));
        com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar), statusColorRes);

        MaterialSwitch switchActive = row.findViewById(R.id.switchActive);
        switchActive.setOnCheckedChangeListener(null);
        switchActive.setChecked(session.isActive());
        switchActive.setEnabled(!isUpdating);
        switchActive.setOnCheckedChangeListener((button, checked) -> viewModel.setSessionActive(session, checked));

        row.findViewById(R.id.clickableArea).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("departmentId", viewModel.getDepartmentId());
            args.putString("sessionId", session.getId());
            NavHostFragment.findNavController(this).navigate(R.id.sessionSemesterListFragment, args);
        });
        row.findViewById(R.id.buttonEdit).setOnClickListener(v -> showSessionFormDialog(session));
        row.findViewById(R.id.buttonDelete).setOnClickListener(v -> viewModel.requestDeleteSession(session));

        return row;
    }

    private void showSessionDeleteDialog(SessionDeleteCheck check, List<Session> sessions) {
        Session session = check.getSession();
        if (check.getLinkedStudentCount() <= 0L) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.delete_session_title_format, session.getLabel()))
                    .setMessage(R.string.cannot_undo)
                    .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteSession(session.getId()))
                    .setNegativeButton(R.string.cancel_button, (d, w) -> viewModel.cancelSessionDelete())
                    .setOnCancelListener(d -> viewModel.cancelSessionDelete())
                    .show();
            return;
        }

        List<Session> otherActiveSessions = new ArrayList<>();
        for (Session candidate : sessions) {
            if (!candidate.getId().equals(session.getId()) && candidate.isActive()) otherActiveSessions.add(candidate);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.cannot_delete_session_title_format, session.getLabel()))
                .setMessage(otherActiveSessions.isEmpty()
                        ? getString(R.string.cannot_delete_session_message_no_target, check.getLinkedStudentCount())
                        : getString(R.string.cannot_delete_session_message_with_target, check.getLinkedStudentCount()))
                .setNegativeButton(R.string.cancel_button, (d, w) -> viewModel.cancelSessionDelete())
                .setOnCancelListener(d -> viewModel.cancelSessionDelete());
        if (!otherActiveSessions.isEmpty()) {
            builder.setPositiveButton(R.string.reassign_and_delete, (d, w) ->
                    SelectDialogHelper.show(requireContext(), getString(R.string.move_students_to_title),
                            otherActiveSessions, Session::getLabel, "",
                            target -> viewModel.reassignAndDeleteSession(session.getId(), target.getId())));
        }
        builder.show();
    }

    private void showSessionFormDialog(@Nullable Session initial) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_session_form, null);
        TextInputLayout layoutLabel = dialogView.findViewById(R.id.layoutLabel);
        TextInputEditText editLabel = dialogView.findViewById(R.id.editLabel);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        if (initial != null) editLabel.setText(initial.getLabel());

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(initial == null ? R.string.add_session_title : R.string.edit_session_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String label = editLabel.getText() == null ? "" : editLabel.getText().toString().trim();
            if (label.isEmpty()) {
                textError.setText(R.string.session_label_required);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            textError.setVisibility(View.GONE);
            progressSaving.setVisibility(View.VISIBLE);
            layoutLabel.setEnabled(false);

            var task = initial == null ? viewModel.addSession(label) : viewModel.editSessionLabel(initial.getId(), label);
            task.addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    dialog.dismiss();
                } else {
                    progressSaving.setVisibility(View.GONE);
                    layoutLabel.setEnabled(true);
                    textError.setText(result.getException() != null ? result.getException().getMessage() : null);
                    textError.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showDepartmentFormDialog(Department department) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_department_form, null);
        TextInputLayout layoutName = dialogView.findViewById(R.id.layoutName);
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        TextInputLayout layoutCode = dialogView.findViewById(R.id.layoutCode);
        TextInputEditText editCode = dialogView.findViewById(R.id.editCode);
        TextInputEditText editDescription = dialogView.findViewById(R.id.editDescription);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        editName.setText(department.getName());
        editCode.setText(department.getCode());
        editDescription.setText(department.getDescription());

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_department_title)
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

            viewModel.updateDepartment(name, code, description).addOnCompleteListener(result -> {
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
