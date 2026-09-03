package com.example.uos_lms.feature.student.presentation.assignment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SubmitAssignmentFragment extends Fragment {

    private SubmitAssignmentViewModel viewModel;
    private ActivityResultLauncher<String> filePicker;
    private TextInputEditText editTextAnswer;
    private boolean suppressTextWatcher;

    public SubmitAssignmentFragment() {
        super(R.layout.fragment_submit_assignment);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) viewModel.onFilePicked(uri);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_submit_assignment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SubmitAssignmentViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        editTextAnswer = view.findViewById(R.id.editTextAnswer);
        editTextAnswer.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!suppressTextWatcher) viewModel.onTextAnswerChange(s.toString());
            }
        });

        MaterialButton buttonAttachFile = view.findViewById(R.id.buttonAttachFile);
        buttonAttachFile.setOnClickListener(v -> filePicker.launch("*/*"));

        MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(v -> viewModel.submit());

        viewModel.getSubmitted().observe(getViewLifecycleOwner(), submitted -> {
            if (submitted) NavHostFragment.findNavController(this).popBackStack();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, SubmitAssignmentUiState state) {
        ((TextView) view.findViewById(R.id.toolbar).findViewById(R.id.textTitle)).setText(
                state.getAssignment() != null ? state.getAssignment().getTitle() : getString(R.string.assignment_fallback_title));

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);
        progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(state.isLoading() ? View.GONE : View.VISIBLE);
        if (state.isLoading()) return;

        Assignment assignment = state.getAssignment();
        AssignmentSubmission submission = state.getExistingSubmission();

        if (assignment != null) {
            ((TextView) view.findViewById(R.id.textDescription)).setText(assignment.getDescription());
            ((TextView) view.findViewById(R.id.textMeta)).setText(getString(R.string.due_marks_format,
                    DateKeyUtils.millisToDisplay(assignment.getDueDateMillis()), assignment.getMaxMarks()));

            TextView textDownload = view.findViewById(R.id.textDownloadAttachment);
            if (assignment.getFileUrl() != null) {
                textDownload.setText(getString(R.string.download_attachment_format,
                        assignment.getFileName() != null ? assignment.getFileName() : getString(R.string.attachment_fallback_name)));
                textDownload.setVisibility(View.VISIBLE);
                textDownload.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(assignment.getFileUrl()))));
            } else {
                textDownload.setVisibility(View.GONE);
            }
        }

        TextView textError = view.findViewById(R.id.textError);
        if (state.getErrorMessage() != null) {
            textError.setText(state.getErrorMessage());
            textError.setVisibility(View.VISIBLE);
        } else {
            textError.setVisibility(View.GONE);
        }

        View gradedCard = view.findViewById(R.id.gradedCard);
        View formContainer = view.findViewById(R.id.formContainer);
        boolean isGraded = submission != null && submission.isGraded();
        gradedCard.setVisibility(isGraded ? View.VISIBLE : View.GONE);
        formContainer.setVisibility(isGraded ? View.GONE : View.VISIBLE);

        if (isGraded) {
            TextView textGradedAnswer = view.findViewById(R.id.textGradedAnswer);
            if (submission.getTextAnswer() != null && !submission.getTextAnswer().isEmpty()) {
                textGradedAnswer.setText(submission.getTextAnswer());
                textGradedAnswer.setVisibility(View.VISIBLE);
            } else {
                textGradedAnswer.setVisibility(View.GONE);
            }

            TextView textGradedFile = view.findViewById(R.id.textGradedFile);
            if (submission.getFileName() != null) {
                textGradedFile.setText(getString(R.string.file_label_format, submission.getFileName()));
                textGradedFile.setVisibility(View.VISIBLE);
                textGradedFile.setOnClickListener(v -> {
                    if (submission.getFileUrl() != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getFileUrl())));
                    }
                });
            } else {
                textGradedFile.setVisibility(View.GONE);
            }

            int maxMarks = assignment != null ? assignment.getMaxMarks() : 0;
            ((TextView) view.findViewById(R.id.textGrade)).setText(
                    getString(R.string.grade_score_format, submission.getMarksObtained(), maxMarks));

            TextView textGradedFeedback = view.findViewById(R.id.textGradedFeedback);
            if (submission.getFeedback() != null && !submission.getFeedback().isBlank()) {
                textGradedFeedback.setText(getString(R.string.feedback_prefix_format, submission.getFeedback()));
                textGradedFeedback.setVisibility(View.VISIBLE);
            } else {
                textGradedFeedback.setVisibility(View.GONE);
            }
        } else {
            TextView textHint = view.findViewById(R.id.textAlreadySubmittedHint);
            if (submission != null) {
                textHint.setText(getString(R.string.submitted_resubmit_hint_format, DateKeyUtils.millisToDisplay(submission.getSubmittedAt())));
                textHint.setVisibility(View.VISIBLE);
            } else {
                textHint.setVisibility(View.GONE);
            }

            suppressTextWatcher = true;
            if (!editTextAnswer.getText().toString().equals(state.getTextAnswer())) {
                editTextAnswer.setText(state.getTextAnswer());
                editTextAnswer.setSelection(editTextAnswer.getText().length());
            }
            suppressTextWatcher = false;

            MaterialButton buttonAttachFile = view.findViewById(R.id.buttonAttachFile);
            if (state.getPickedFileUri() != null) {
                buttonAttachFile.setText(R.string.file_selected_tap_to_change);
            } else if (submission != null && submission.getFileName() != null) {
                buttonAttachFile.setText(getString(R.string.attached_tap_to_replace_format, submission.getFileName()));
            } else {
                buttonAttachFile.setText(R.string.attach_file_button);
            }

            TextView textUploadProgress = view.findViewById(R.id.textUploadProgress);
            if (state.getUploadProgress() != null) {
                textUploadProgress.setVisibility(View.VISIBLE);
                textUploadProgress.setText(getString(R.string.uploading_file_percent_format, state.getUploadProgress()));
            } else {
                textUploadProgress.setVisibility(View.GONE);
            }

            MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);
            buttonSubmit.setText(submission != null ? R.string.resubmit_button : R.string.submit_button);
            boolean submitting = state.isSubmitting();
            buttonSubmit.setEnabled(!submitting);
            CircularProgressIndicator progressSubmitting = view.findViewById(R.id.progressSubmitting);
            progressSubmitting.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
    }
}
