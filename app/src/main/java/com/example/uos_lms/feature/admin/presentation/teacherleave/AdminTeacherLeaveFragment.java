package com.example.uos_lms.feature.admin.presentation.teacherleave;

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

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;
import com.example.uos_lms.core.domain.model.LeaveStatus;
import com.example.uos_lms.core.ui.LeaveStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

/** Admin reviews leave applications submitted by Teachers in every department - see
 * teacherLeaveController.decide (an Admin can decide any department's request). Distinct from
 * AdminLeaveFragment, which is where the Admin reviews STUDENT leave requests, and from
 * HodTeacherLeaveFragment, which is limited to one department. */
@AndroidEntryPoint
public class AdminTeacherLeaveFragment extends Fragment {

    private AdminTeacherLeaveViewModel viewModel;

    public AdminTeacherLeaveFragment() {
        super(R.layout.fragment_admin_teacher_leave);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_teacher_leave, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminTeacherLeaveViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.admin_teacher_leave_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.leave_tab_pending));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.leave_tab_all));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectTab(tab.getPosition() == 0 ? AdminTeacherLeaveReviewTab.PENDING : AdminTeacherLeaveReviewTab.ALL);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<TeacherLeaveApplication> adapter = new SimpleListAdapter<>(R.layout.item_teacher_leave_card, (itemView, leave, position) -> {
            ((TextView) itemView.findViewById(R.id.textTeacherName)).setText(leave.getTeacherName());
            ((TextView) itemView.findViewById(R.id.textLeaveType)).setText(leave.getLeaveType().name());
            ((TextView) itemView.findViewById(R.id.textDateRange)).setText(getString(R.string.leave_date_range_format,
                    DateKeyUtils.millisToDisplay(leave.getFromDateMillis()), DateKeyUtils.millisToDisplay(leave.getToDateMillis())));
            ((TextView) itemView.findViewById(R.id.textReason)).setText(leave.getReason());
            int accentColorRes = LeaveStatusChipHelper.bind(itemView.findViewById(R.id.textStatus), leave.getStatus());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            TextView textRejectionReason = itemView.findViewById(R.id.textRejectionReason);
            if (leave.getRejectionReason() != null && !leave.getRejectionReason().isBlank()) {
                textRejectionReason.setText(getString(R.string.leave_rejection_reason_format, leave.getRejectionReason()));
                textRejectionReason.setVisibility(View.VISIBLE);
            } else {
                textRejectionReason.setVisibility(View.GONE);
            }

            TextView textReviewer = itemView.findViewById(R.id.textReviewer);
            if (leave.getReviewerName() != null) {
                textReviewer.setText(getString(R.string.leave_reviewed_by_format, leave.getReviewerName(), leave.getReviewerRole()));
                textReviewer.setVisibility(View.VISIBLE);
            } else {
                textReviewer.setVisibility(View.GONE);
            }

            View actions = itemView.findViewById(R.id.decisionActions);
            boolean pending = leave.getStatus() == LeaveStatus.PENDING;
            actions.setVisibility(pending ? View.VISIBLE : View.GONE);
            if (pending) {
                boolean processing = leave.getId().equals(viewModel.getUiState().getValue().getProcessingLeaveId());
                itemView.findViewById(R.id.buttonApprove).setEnabled(!processing);
                itemView.findViewById(R.id.buttonReject).setEnabled(!processing);
                itemView.findViewById(R.id.buttonApprove).setOnClickListener(v -> viewModel.approve(leave));
                itemView.findViewById(R.id.buttonReject).setOnClickListener(v -> showRejectDialog(leave));
            }
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            boolean hasLeaves = !state.getVisibleLeaves().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasLeaves ? View.VISIBLE : View.GONE);
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(
                    state.getTab() == AdminTeacherLeaveReviewTab.PENDING ? R.string.no_pending_leave_message : R.string.no_leave_applications_message);
            recyclerList.setVisibility(hasLeaves ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getVisibleLeaves());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void showRejectDialog(TeacherLeaveApplication leave) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reject_teacher_leave, null);
        TextInputEditText editReason = dialogView.findViewById(R.id.editRejectionReason);
        TextView textError = dialogView.findViewById(R.id.textError);

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reject_leave_title)
                .setView(dialogView)
                .setPositiveButton(R.string.leave_reject_button, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            String reason = editReason.getText() == null ? "" : editReason.getText().toString().trim();
            if (reason.isEmpty()) {
                textError.setText(R.string.rejection_reason_required_error);
                textError.setVisibility(View.VISIBLE);
                return;
            }
            viewModel.reject(leave, reason);
            dialog.dismiss();
        });
    }
}
