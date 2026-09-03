package com.example.uos_lms.feature.admin.presentation.usermanagement.student;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Department;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminStudentTreeFragment extends Fragment {

    private AdminStudentTreeViewModel viewModel;
    private boolean suppressDeptSearchWatcher;

    public AdminStudentTreeFragment() {
        super(R.layout.fragment_admin_student_tree);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_student_tree, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminStudentTreeViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.student_tree_fallback_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        TextInputEditText editDepartmentSearch = view.findViewById(R.id.editDepartmentSearch);
        editDepartmentSearch.addTextChangedListener(watcher(text -> {
            if (!suppressDeptSearchWatcher) viewModel.onDepartmentSearchChange(text);
        }));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_departments_found);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.scrollNodes);
        swipeRefresh.setColorSchemeResources(
                R.color.role_admin_start, R.color.role_hod_start, R.color.role_teacher_start, R.color.role_student_start);
        swipeRefresh.setOnRefreshListener(viewModel::refresh);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, editDepartmentSearch, emptyState, state));
    }

    private void render(View view, TextInputEditText editDepartmentSearch, View emptyState, AdminStudentTreeUiState state) {
        suppressDeptSearchWatcher = true;
        String current = editDepartmentSearch.getText() == null ? "" : editDepartmentSearch.getText().toString();
        if (!current.equals(state.getDepartmentSearchQuery())) {
            editDepartmentSearch.setText(state.getDepartmentSearchQuery());
        }
        suppressDeptSearchWatcher = false;

        LinearLayout statsRow = view.findViewById(R.id.statsRow);
        statsRow.removeAllViews();
        addStatPill(statsRow, state.getNodes().size() + " " + getString(R.string.stat_departments));
        addStatPill(statsRow, (state.getTotalSemesters() != null ? state.getTotalSemesters() : "…") + " " + getString(R.string.stat_semesters));
        addStatPill(statsRow, (state.getTotalSessions() != null ? state.getTotalSessions() : "…") + " " + getString(R.string.stat_sessions));
        addStatPill(statsRow, (state.getTotalStudents() != null ? state.getTotalStudents() : "…") + " " + getString(R.string.stat_students));

        boolean hasNodes = !state.getFilteredNodes().isEmpty();
        emptyState.setVisibility(hasNodes || state.isLoadingDepartments() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.scrollNodes).setVisibility(hasNodes ? View.VISIBLE : View.GONE);

        LinearLayout nodesContainer = view.findViewById(R.id.nodesContainer);
        nodesContainer.removeAllViews();
        for (DepartmentStudentNode node : state.getFilteredNodes()) {
            nodesContainer.addView(buildNodeView(nodesContainer, node));
        }

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.scrollNodes);
        if (swipeRefresh.isRefreshing() != state.isRefreshing()) swipeRefresh.setRefreshing(state.isRefreshing());

        if (state.getErrorMessage() != null) {
            Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            viewModel.consumeMessages();
        } else if (state.getActionMessage() != null) {
            Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
            viewModel.consumeMessages();
        }
    }

    private void addStatPill(LinearLayout container, String text) {
        TextView pill = new TextView(requireContext());
        pill.setText(text);
        pill.setTextSize(12f);
        pill.setBackgroundResource(R.drawable.bg_stat_pill);
        int hPad = (int) (12 * getResources().getDisplayMetrics().density);
        int vPad = (int) (6 * getResources().getDisplayMetrics().density);
        pill.setPadding(hPad, vPad, hPad, vPad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(hPad);
        pill.setLayoutParams(params);
        container.addView(pill);
    }

    private View buildNodeView(LinearLayout parent, DepartmentStudentNode node) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.item_department_session_node, parent, false);
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

    private void bindExpandedContent(View view, DepartmentStudentNode node) {
        String departmentId = node.getDepartment().getId();

        TextInputEditText editSearch = view.findViewById(R.id.editSearch);
        editSearch.setText(node.getSessionSearchQuery());
        editSearch.addTextChangedListener(watcher(text -> viewModel.onSessionSearchChange(departmentId, text)));

        view.findViewById(R.id.buttonAddSession).setOnClickListener(v -> showAddSessionDialog(departmentId));

        TextView statusMessage = view.findViewById(R.id.textStatusMessage);
        LinearLayout sessionListContainer = view.findViewById(R.id.sessionListContainer);
        sessionListContainer.removeAllViews();

        if (node.isLoadingSessions()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.loading_ellipsis);
        } else if (node.getFilteredSessions().isEmpty()) {
            statusMessage.setVisibility(View.VISIBLE);
            statusMessage.setText(R.string.no_sessions_yet_short);
        } else {
            statusMessage.setVisibility(View.GONE);
            for (SessionNode sessionNode : node.getFilteredSessions()) {
                sessionListContainer.addView(buildSessionRow(sessionListContainer, departmentId, sessionNode));
            }
        }
    }

    private View buildSessionRow(LinearLayout parent, String departmentId, SessionNode sessionNode) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_session_row, parent, false);
        ((TextView) row.findViewById(R.id.textLabel)).setText(sessionNode.getSession().getLabel());
        TextView textSubtitle = row.findViewById(R.id.textSubtitle);
        textSubtitle.setVisibility(sessionNode.getSession().isActive() ? View.GONE : View.VISIBLE);
        textSubtitle.setText(R.string.inactive_status);
        ((TextView) row.findViewById(R.id.textCountBadge)).setText(
                sessionNode.getTotalCount() != null ? String.valueOf(sessionNode.getTotalCount()) : "…");
        com.example.uos_lms.core.ui.AccentColors.applyBar(row.findViewById(R.id.accentBar), R.color.role_admin_start);

        row.findViewById(R.id.clickableArea).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("departmentId", departmentId);
            args.putString("sessionId", sessionNode.getSession().getId());
            NavHostFragment.findNavController(this).navigate(R.id.adminStudentSemesterListFragment, args);
        });

        return row;
    }

    private void showAddSessionDialog(String departmentId) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_session_form, null);
        TextInputEditText editLabel = dialogView.findViewById(R.id.editLabel);
        TextView textError = dialogView.findViewById(R.id.textError);
        CircularProgressIndicator progressSaving = dialogView.findViewById(R.id.progressSaving);

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_session_title)
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
            editLabel.setEnabled(false);

            viewModel.createSession(departmentId, label).addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    dialog.dismiss();
                } else {
                    progressSaving.setVisibility(View.GONE);
                    editLabel.setEnabled(true);
                    textError.setText(result.getException() != null ? result.getException().getMessage() : null);
                    textError.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private TextWatcher watcher(java.util.function.Consumer<String> onChanged) {
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
