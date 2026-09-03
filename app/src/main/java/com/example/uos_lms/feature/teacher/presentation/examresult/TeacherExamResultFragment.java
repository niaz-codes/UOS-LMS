package com.example.uos_lms.feature.teacher.presentation.examresult;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherExamResultFragment extends Fragment {

    private TeacherExamResultViewModel viewModel;
    private TextInputEditText editTotalMarks;
    private boolean suppressTotalMarksWatcher;

    public TeacherExamResultFragment() {
        super(R.layout.fragment_teacher_exam_result);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_exam_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherExamResultViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.exam_result_entry_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_approved_students_message);
        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View contentContainer = view.findViewById(R.id.contentContainer);

        editTotalMarks = view.findViewById(R.id.editTotalMarks);
        editTotalMarks.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressTotalMarksWatcher) viewModel.updateTotalMarks(s);
        }));

        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<ExamResultRow> adapter = new SimpleListAdapter<>(R.layout.item_teacher_exam_result_row, (itemView, row, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(row.getStudent().getFullName());
            String rollOrEmail = row.getStudent().getRollNumber() != null && !row.getStudent().getRollNumber().isEmpty()
                    ? row.getStudent().getRollNumber() : row.getStudent().getEmail();
            ((TextView) itemView.findViewById(R.id.textRollOrEmail)).setText(rollOrEmail);

            TextView textStatusChip = itemView.findViewById(R.id.textStatusChip);
            TextView textRejectionReason = itemView.findViewById(R.id.textRejectionReason);
            View accentBar = itemView.findViewById(R.id.accentBar);
            ExamResult existing = row.getExisting();
            if (existing != null && existing.getStatus() != ResultStatus.DRAFT) {
                textStatusChip.setVisibility(View.VISIBLE);
                com.example.uos_lms.core.ui.AccentColors.applyBar(accentBar, ResultStatusChipHelper.bind(textStatusChip, existing.getStatus()));
            } else {
                textStatusChip.setVisibility(View.GONE);
                com.example.uos_lms.core.ui.AccentColors.applyBar(accentBar, R.color.on_surface_variant_color);
            }
            if (existing != null && existing.getRejectionReason() != null) {
                textRejectionReason.setText(getString(R.string.reason_label_format, existing.getRejectionReason()));
                textRejectionReason.setVisibility(View.VISIBLE);
            } else {
                textRejectionReason.setVisibility(View.GONE);
            }

            TextInputEditText editMarks = itemView.findViewById(R.id.editMarks);
            Object previousWatcher = editMarks.getTag();
            if (previousWatcher instanceof TextWatcher) {
                editMarks.removeTextChangedListener((TextWatcher) previousWatcher);
            }
            if (!editMarks.getText().toString().equals(row.getMarksInput())) {
                editMarks.setText(row.getMarksInput());
                editMarks.setSelection(editMarks.getText().length());
            }
            editMarks.setEnabled(row.isEditable());
            TextWatcher watcher = new SimpleTextWatcher(s -> viewModel.updateMarks(row.getStudent().getUid(), s));
            editMarks.addTextChangedListener(watcher);
            editMarks.setTag(watcher);
        });
        recyclerList.setAdapter(adapter);

        MaterialButton buttonSaveDraft = view.findViewById(R.id.buttonSaveDraft);
        MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);
        CircularProgressIndicator progressSaving = view.findViewById(R.id.progressSaving);
        buttonSaveDraft.setOnClickListener(v -> viewModel.saveDraft());
        buttonSubmit.setOnClickListener(v -> viewModel.submitForApproval());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasRows = !state.getRows().isEmpty();
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            emptyState.setVisibility(!state.isLoading() && !hasRows ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(!state.isLoading() && hasRows ? View.VISIBLE : View.GONE);

            suppressTotalMarksWatcher = true;
            if (!editTotalMarks.getText().toString().equals(state.getTotalMarksInput())) {
                editTotalMarks.setText(state.getTotalMarksInput());
                editTotalMarks.setSelection(editTotalMarks.getText().length());
            }
            suppressTotalMarksWatcher = false;
            editTotalMarks.setEnabled(!state.isTotalMarksLocked());

            adapter.submitList(state.getRows());

            boolean saving = state.isSaving();
            buttonSaveDraft.setEnabled(!saving);
            buttonSubmit.setEnabled(state.canSubmit() && !saving);
            progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            }
        });
    }

    private interface TextChangedCallback {
        void onChanged(String text);
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextChangedCallback callback;

        SimpleTextWatcher(TextChangedCallback callback) {
            this.callback = callback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            callback.onChanged(s.toString());
        }
    }
}
