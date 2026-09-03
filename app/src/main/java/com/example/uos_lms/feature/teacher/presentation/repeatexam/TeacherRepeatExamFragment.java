package com.example.uos_lms.feature.teacher.presentation.repeatexam;

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
import com.example.uos_lms.core.domain.model.RepeatStatus;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TeacherRepeatExamFragment extends Fragment {

    private TeacherRepeatExamViewModel viewModel;

    public TeacherRepeatExamFragment() {
        super(R.layout.fragment_teacher_repeat_exam);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_repeat_exam, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherRepeatExamViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.repeat_exam_entry_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        ((TextView) view.findViewById(R.id.textSubjectLabel)).setText(
                getString(R.string.subject_code_title_format, viewModel.getSubjectCode(), viewModel.getSubjectTitle()));

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_repeat_eligible_students_message);
        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<RepeatExamRow> adapter = new SimpleListAdapter<>(R.layout.item_teacher_repeat_exam_row, (itemView, row, position) -> {
            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(row.getExamResult().getStudentName());
            ((TextView) itemView.findViewById(R.id.textPreviousAttempt)).setText(getString(R.string.previous_attempt_format,
                    row.getExamResult().getObtainedMarks(), row.getExamResult().getGrade()));

            TextView textStatus = itemView.findViewById(R.id.textStatus);
            TextView textRejectionReason = itemView.findViewById(R.id.textRejectionReason);
            View entryRow = itemView.findViewById(R.id.entryRow);
            boolean submitted = row.getLatestRepeat() != null && row.getLatestRepeat().getRepeatStatus() == RepeatStatus.SUBMITTED;

            if (submitted) {
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText(getString(R.string.new_attempt_format,
                        row.getLatestRepeat().getNewMarks().intValue(), row.getLatestRepeat().getNewGrade())
                        + " — " + getString(R.string.awaiting_hod_approval_label));
            } else {
                textStatus.setVisibility(View.GONE);
            }

            boolean rejected = row.getLatestRepeat() != null && row.getLatestRepeat().getRepeatStatus() == RepeatStatus.REJECTED
                    && row.getLatestRepeat().getRejectionReason() != null;
            if (rejected) {
                textRejectionReason.setVisibility(View.VISIBLE);
                textRejectionReason.setText(getString(R.string.reason_label_format, row.getLatestRepeat().getRejectionReason()));
            } else {
                textRejectionReason.setVisibility(View.GONE);
            }
            int accentColorRes = rejected ? R.color.error_color : submitted ? R.color.status_warning : R.color.on_surface_variant_color;
            com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), accentColorRes);

            entryRow.setVisibility(row.isEditable() ? View.VISIBLE : View.GONE);

            TextInputEditText editMarks = itemView.findViewById(R.id.editMarks);
            Object previousWatcher = editMarks.getTag();
            if (previousWatcher instanceof TextWatcher) {
                editMarks.removeTextChangedListener((TextWatcher) previousWatcher);
            }
            if (!editMarks.getText().toString().equals(row.getMarksInput())) {
                editMarks.setText(row.getMarksInput());
                editMarks.setSelection(editMarks.getText().length());
            }
            TextWatcher watcher = new SimpleTextWatcher(s -> viewModel.updateMarks(row.getExamResult().getId(), s));
            editMarks.addTextChangedListener(watcher);
            editMarks.setTag(watcher);

            boolean processing = row.getExamResult().getId().equals(viewModel.getUiState().getValue().getProcessingExamResultId());
            MaterialButton buttonSubmit = itemView.findViewById(R.id.buttonSubmit);
            buttonSubmit.setEnabled(!processing);
            buttonSubmit.setOnClickListener(v -> viewModel.submit(row.getExamResult().getId()));
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasRows = !state.getRows().isEmpty();
            progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            emptyState.setVisibility(!state.isLoading() && !hasRows ? View.VISIBLE : View.GONE);
            recyclerList.setVisibility(!state.isLoading() && hasRows ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getRows());

            if (state.getErrorMessage() != null) {
                Snackbar.make(view, state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.consumeMessages();
            } else if (state.getActionMessage() != null) {
                Snackbar.make(view, state.getActionMessage(), Snackbar.LENGTH_SHORT).show();
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
