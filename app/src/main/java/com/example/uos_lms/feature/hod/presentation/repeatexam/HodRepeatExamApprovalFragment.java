package com.example.uos_lms.feature.hod.presentation.repeatexam;

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
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HodRepeatExamApprovalFragment extends Fragment {

    private HodRepeatExamApprovalViewModel viewModel;

    public HodRepeatExamApprovalFragment() {
        super(R.layout.fragment_hod_repeat_exam_review);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hod_repeat_exam_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HodRepeatExamApprovalViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.repeat_exam_review_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_pending_repeat_exams);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<RepeatExam> adapter = new SimpleListAdapter<>(R.layout.item_repeat_exam_approval, (itemView, repeatExam, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(repeatExam.getStudentName());
            ((TextView) itemView.findViewById(R.id.textSubject)).setText(
                    getString(R.string.subject_code_title_format, repeatExam.getSubjectCode(), repeatExam.getSubjectTitle()));
            ((TextView) itemView.findViewById(R.id.textPreviousAttempt)).setText(getString(R.string.previous_attempt_format,
                    (int) repeatExam.getPreviousMarks(), repeatExam.getPreviousGrade()));
            ((TextView) itemView.findViewById(R.id.textNewAttempt)).setText(getString(R.string.new_attempt_format,
                    repeatExam.getNewMarks() != null ? repeatExam.getNewMarks().intValue() : 0, repeatExam.getNewGrade()));
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_warning);

            boolean processing = repeatExam.getId().equals(viewModel.getUiState().getValue().getProcessingRepeatExamId());
            itemView.findViewById(R.id.buttonReject).setEnabled(!processing);
            itemView.findViewById(R.id.buttonApprove).setEnabled(!processing);
            itemView.findViewById(R.id.buttonReject).setOnClickListener(v -> showRejectDialog(repeatExam));
            itemView.findViewById(R.id.buttonApprove).setOnClickListener(v -> viewModel.approve(repeatExam));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            boolean hasResults = !state.getPending().isEmpty();
            emptyState.setVisibility(!state.isLoading() && !hasResults ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(!state.isLoading() && hasResults ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getPending());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessages();
            }
        });
    }

    private void showRejectDialog(RepeatExam repeatExam) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reject_reason, null);
        TextInputEditText editReason = dialogView.findViewById(R.id.editReason);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.reject_dialog_title_format, repeatExam.getStudentName()))
                .setView(dialogView)
                .setPositiveButton(R.string.reject_button, (dialog, which) -> {
                    String reason = editReason.getText() == null ? "" : editReason.getText().toString().trim();
                    if (!reason.isEmpty()) viewModel.reject(repeatExam, reason);
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
}
