package com.example.uos_lms.feature.teacher.presentation.assignment;

import android.content.Intent;
import android.net.Uri;
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

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AssignmentSubmissionsFragment extends Fragment {

    private AssignmentSubmissionsViewModel viewModel;
    private int maxMarks;

    public AssignmentSubmissionsFragment() {
        super(R.layout.fragment_assignment_submissions);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assignment_submissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AssignmentSubmissionsViewModel.class);

        Bundle args = requireArguments();
        String title = args.getString("title");
        maxMarks = args.getInt("maxMarks");

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_submissions_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<AssignmentSubmission> adapter = new SimpleListAdapter<>(R.layout.item_assignment_submission, (itemView, submission, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(submission.getStudentName());

            TextView textAnswer = itemView.findViewById(R.id.textAnswer);
            if (submission.getTextAnswer() != null && !submission.getTextAnswer().isEmpty()) {
                textAnswer.setText(submission.getTextAnswer());
                textAnswer.setVisibility(View.VISIBLE);
            } else {
                textAnswer.setVisibility(View.GONE);
            }

            TextView textFileName = itemView.findViewById(R.id.textFileName);
            if (submission.getFileName() != null) {
                textFileName.setText(getString(R.string.file_label_format, submission.getFileName()));
                textFileName.setVisibility(View.VISIBLE);
                textFileName.setOnClickListener(v -> {
                    if (submission.getFileUrl() != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getFileUrl())));
                    }
                });
            } else {
                textFileName.setVisibility(View.GONE);
                textFileName.setOnClickListener(null);
            }

            TextView textStatus = itemView.findViewById(R.id.textStatus);
            com.google.android.material.button.MaterialButton buttonGrade = itemView.findViewById(R.id.buttonGrade);
            if (submission.isGraded()) {
                textStatus.setText(getString(R.string.graded_format, submission.getMarksObtained(), maxMarks));
                textStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                        itemView, com.google.android.material.R.attr.colorPrimary));
                buttonGrade.setText(R.string.update_grade);
                com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_success);
            } else {
                textStatus.setText(R.string.pending_grading);
                textStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                        itemView, com.google.android.material.R.attr.colorError));
                buttonGrade.setText(R.string.grade_button);
                com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_warning);
            }
            buttonGrade.setOnClickListener(v -> showGradeDialog(submission));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasSubmissions = !state.getSubmissions().isEmpty();
            emptyState.setVisibility(hasSubmissions || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasSubmissions ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getSubmissions());
        });
    }

    private void showGradeDialog(AssignmentSubmission submission) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade_submission, null);
        com.google.android.material.textfield.TextInputLayout layoutMarks = dialogView.findViewById(R.id.layoutMarks);
        TextInputEditText editMarks = dialogView.findViewById(R.id.editMarks);
        TextInputEditText editFeedback = dialogView.findViewById(R.id.editFeedback);
        TextView textError = dialogView.findViewById(R.id.textError);

        layoutMarks.setHint(getString(R.string.marks_out_of_format, maxMarks));
        if (submission.getMarksObtained() != null) editMarks.setText(String.valueOf(submission.getMarksObtained()));
        if (submission.getFeedback() != null) editFeedback.setText(submission.getFeedback());

        android.app.Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.grade_submission_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel_button, null)
                .show();

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            Integer marks = null;
            try {
                marks = Integer.parseInt(editMarks.getText() == null ? "" : editMarks.getText().toString().trim());
            } catch (NumberFormatException ignored) {
                // handled below via null check
            }
            if (marks == null || marks < 0 || marks > maxMarks) {
                textError.setText(getString(R.string.marks_range_error_format, maxMarks));
                textError.setVisibility(View.VISIBLE);
                return;
            }
            String feedback = editFeedback.getText() == null ? "" : editFeedback.getText().toString().trim();
            viewModel.grade(submission, marks, feedback);
            dialog.dismiss();
        });
    }
}
