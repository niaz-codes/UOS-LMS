package com.example.uos_lms.feature.hod.presentation.examresult;

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
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.ui.ResultStatusChipHelper;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodExamResultApprovalFragment extends Fragment {

    private HodExamResultApprovalViewModel viewModel;

    public HodExamResultApprovalFragment() {
        super(R.layout.fragment_hod_examresult_approvals);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_examresult_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodExamResultApprovalViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.exam_result_approvals_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.pending_count_format, 0)));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectTab(tab.getPosition() == 0 ? ResultReviewTab.PENDING : ResultReviewTab.ALL);
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

        SimpleListAdapter<ExamResult> adapter = new SimpleListAdapter<>(R.layout.item_exam_result_approval, (itemView, result, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(result.getStudentName());
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(
                    result.getSubjectCode() + " • " + result.getSubjectTitle());
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                    ResultStatusChipHelper.bind(itemView.findViewById(R.id.textStatusChip), result.getStatus()));
            ((TextView) itemView.findViewById(R.id.textMarks)).setText(getString(R.string.marks_percentage_format,
                    result.getObtainedMarks(), result.getTotalMarks(), result.getPercentage()));
            ((TextView) itemView.findViewById(R.id.textTeacher)).setText(getString(R.string.teacher_label_format, result.getTeacherName()));

            TextView textGrade = itemView.findViewById(R.id.textGrade);
            if (result.getStatus() == ResultStatus.APPROVED && result.getGrade() != null && result.getGpaPoint() != null) {
                textGrade.setVisibility(View.VISIBLE);
                textGrade.setText(getString(R.string.grade_gpa_format, result.getGrade(), result.getGpaPoint()));
            } else {
                textGrade.setVisibility(View.GONE);
            }

            TextView textRejectionReason = itemView.findViewById(R.id.textRejectionReason);
            if (result.getStatus() == ResultStatus.REJECTED && result.getRejectionReason() != null) {
                textRejectionReason.setVisibility(View.VISIBLE);
                textRejectionReason.setText(getString(R.string.reason_label_format, result.getRejectionReason()));
            } else {
                textRejectionReason.setVisibility(View.GONE);
            }

            View actionsRow = itemView.findViewById(R.id.actionsRow);
            boolean pendingApproval = result.getStatus() == ResultStatus.PENDING_HOD_APPROVAL;
            actionsRow.setVisibility(pendingApproval ? View.VISIBLE : View.GONE);
            if (pendingApproval) {
                boolean processing = result.getId().equals(viewModel.getUiState().getValue().getProcessingResultId());
                itemView.findViewById(R.id.buttonReject).setEnabled(!processing);
                itemView.findViewById(R.id.buttonApprove).setEnabled(!processing);
                itemView.findViewById(R.id.buttonReject).setOnClickListener(v -> showRejectDialog(result));
                itemView.findViewById(R.id.buttonApprove).setOnClickListener(v -> viewModel.approve(result));
            }
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            TabLayout.Tab pendingTab = tabLayout.getTabAt(0);
            if (pendingTab != null) pendingTab.setText(getString(R.string.pending_count_format, state.getPending().size()));

            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasResults = !state.getVisibleResults().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasResults ? View.VISIBLE : View.GONE);
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(
                    state.getTab() == ResultReviewTab.PENDING ? R.string.no_pending_results : R.string.no_results_in_department);
            recyclerList.setVisibility(!state.isLoading() && hasResults ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getVisibleResults());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void showRejectDialog(ExamResult result) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reject_reason, null);
        TextInputEditText editReason = dialogView.findViewById(R.id.editReason);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.reject_dialog_title_format, result.getStudentName()))
                .setView(dialogView)
                .setPositiveButton(R.string.reject_button, (dialog, which) -> {
                    String reason = editReason.getText() == null ? "" : editReason.getText().toString().trim();
                    if (!reason.isEmpty()) viewModel.reject(result, reason);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
}
