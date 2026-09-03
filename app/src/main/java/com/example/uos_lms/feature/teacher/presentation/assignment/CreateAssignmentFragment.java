package com.example.uos_lms.feature.teacher.presentation.assignment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateAssignmentFragment extends Fragment {

    private CreateAssignmentViewModel viewModel;
    private ActivityResultLauncher<String> filePicker;

    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private TextInputEditText editMaxMarks;
    private MaterialButton buttonPickDueDate;
    private MaterialButton buttonAttachFile;
    private MaterialButton buttonSave;
    private CircularProgressIndicator progressSaving;
    private TextView textUploadProgress;
    private TextView textError;

    private boolean suppressWatchers;

    public CreateAssignmentFragment() {
        super(R.layout.fragment_create_assignment);
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
        return inflater.inflate(R.layout.fragment_create_assignment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateAssignmentViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.create_assignment_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        editTitle = view.findViewById(R.id.editTitle);
        editDescription = view.findViewById(R.id.editDescription);
        editMaxMarks = view.findViewById(R.id.editMaxMarks);
        buttonPickDueDate = view.findViewById(R.id.buttonPickDueDate);
        buttonAttachFile = view.findViewById(R.id.buttonAttachFile);
        buttonSave = view.findViewById(R.id.buttonSave);
        progressSaving = view.findViewById(R.id.progressSaving);
        textUploadProgress = view.findViewById(R.id.textUploadProgress);
        textError = view.findViewById(R.id.textError);

        editTitle.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressWatchers) viewModel.onTitleChange(s);
        }));
        editDescription.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressWatchers) viewModel.onDescriptionChange(s);
        }));
        editMaxMarks.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressWatchers) viewModel.onMaxMarksChange(s);
        }));

        buttonPickDueDate.setOnClickListener(v -> showDatePicker());
        buttonAttachFile.setOnClickListener(v -> filePicker.launch("*/*"));
        buttonSave.setOnClickListener(v -> viewModel.save());

        viewModel.getCreated().observe(getViewLifecycleOwner(), created -> {
            if (created) NavHostFragment.findNavController(this).popBackStack();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(CreateAssignmentUiState state) {
        suppressWatchers = true;
        if (!editTitle.getText().toString().equals(state.getTitle())) {
            editTitle.setText(state.getTitle());
            editTitle.setSelection(editTitle.getText().length());
        }
        if (!editDescription.getText().toString().equals(state.getDescription())) {
            editDescription.setText(state.getDescription());
            editDescription.setSelection(editDescription.getText().length());
        }
        if (!editMaxMarks.getText().toString().equals(state.getMaxMarksText())) {
            editMaxMarks.setText(state.getMaxMarksText());
            editMaxMarks.setSelection(editMaxMarks.getText().length());
        }
        suppressWatchers = false;

        buttonPickDueDate.setText(state.getDueDateMillis() != null
                ? getString(R.string.due_label_format, DateKeyUtils.millisToDisplay(state.getDueDateMillis()))
                : getString(R.string.pick_due_date));

        buttonAttachFile.setText(state.getPickedFileUri() != null
                ? R.string.file_attached_tap_to_change
                : R.string.attach_file_optional);

        if (state.getUploadProgress() != null) {
            textUploadProgress.setVisibility(View.VISIBLE);
            textUploadProgress.setText(getString(R.string.uploading_file_percent_format, state.getUploadProgress()));
        } else {
            textUploadProgress.setVisibility(View.GONE);
        }

        if (state.getErrorMessage() != null) {
            textError.setVisibility(View.VISIBLE);
            textError.setText(state.getErrorMessage());
        } else {
            textError.setVisibility(View.GONE);
        }

        boolean saving = state.isSaving();
        progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);
        buttonSave.setEnabled(!saving);
        editTitle.setEnabled(!saving);
        editDescription.setEnabled(!saving);
        editMaxMarks.setEnabled(!saving);
        buttonPickDueDate.setEnabled(!saving);
        buttonAttachFile.setEnabled(!saving);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        Long existing = viewModel.getUiState().getValue() != null ? viewModel.getUiState().getValue().getDueDateMillis() : null;
        if (existing != null) calendar.setTimeInMillis(existing);

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            viewModel.onDueDateChange(picked.getTimeInMillis());
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private interface TextChangeListener {
        void onChanged(String value);
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final TextChangeListener listener;

        SimpleTextWatcher(TextChangeListener listener) {
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            listener.onChanged(s.toString());
        }
    }
}
