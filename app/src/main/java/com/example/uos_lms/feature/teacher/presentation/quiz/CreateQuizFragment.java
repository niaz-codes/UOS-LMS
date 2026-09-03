package com.example.uos_lms.feature.teacher.presentation.quiz;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.domain.model.QuestionType;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateQuizFragment extends Fragment {

    private CreateQuizViewModel viewModel;

    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private TextInputEditText editTimeLimit;
    private MaterialButton buttonPickDueDate;
    private LinearLayout questionsContainer;
    private MaterialButton buttonAddQuestion;
    private TextView textError;
    private MaterialButton buttonSave;
    private CircularProgressIndicator progressSaving;

    private boolean suppressTopWatchers;
    private final List<QuestionCardRefs> questionCardRefs = new ArrayList<>();

    public CreateQuizFragment() {
        super(R.layout.fragment_create_quiz);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateQuizViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(R.string.create_quiz_title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        editTitle = view.findViewById(R.id.editTitle);
        editDescription = view.findViewById(R.id.editDescription);
        editTimeLimit = view.findViewById(R.id.editTimeLimit);
        buttonPickDueDate = view.findViewById(R.id.buttonPickDueDate);
        questionsContainer = view.findViewById(R.id.questionsContainer);
        buttonAddQuestion = view.findViewById(R.id.buttonAddQuestion);
        textError = view.findViewById(R.id.textError);
        buttonSave = view.findViewById(R.id.buttonSave);
        progressSaving = view.findViewById(R.id.progressSaving);

        editTitle.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressTopWatchers) viewModel.onTitleChange(s);
        }));
        editDescription.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressTopWatchers) viewModel.onDescriptionChange(s);
        }));
        editTimeLimit.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!suppressTopWatchers) viewModel.onTimeLimitChange(s);
        }));

        buttonPickDueDate.setOnClickListener(v -> showDatePicker());
        buttonAddQuestion.setOnClickListener(v -> viewModel.addQuestion());
        buttonSave.setOnClickListener(v -> viewModel.save());

        viewModel.getCreated().observe(getViewLifecycleOwner(), created -> {
            if (created) NavHostFragment.findNavController(this).popBackStack();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(CreateQuizUiState state) {
        suppressTopWatchers = true;
        diffSetText(editTitle, state.getTitle());
        diffSetText(editDescription, state.getDescription());
        diffSetText(editTimeLimit, state.getTimeLimitText());
        suppressTopWatchers = false;

        buttonPickDueDate.setText(state.getDueDateMillis() != null
                ? getString(R.string.due_label_format, DateKeyUtils.millisToDisplay(state.getDueDateMillis()))
                : getString(R.string.pick_due_date));

        renderQuestions(state.getQuestions());

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
        editTimeLimit.setEnabled(!saving);
        buttonPickDueDate.setEnabled(!saving);
        buttonAddQuestion.setEnabled(!saving);
    }

    private void renderQuestions(List<QuestionDraft> questions) {
        boolean canRemove = questions.size() > 1;

        if (questionCardRefs.size() != questions.size()) {
            questionsContainer.removeAllViews();
            questionCardRefs.clear();
            for (int i = 0; i < questions.size(); i++) {
                int index = i;
                View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_quiz_question_editor, questionsContainer, false);
                QuestionCardRefs refs = new QuestionCardRefs(card);
                refs.optionLayouts[0].setHint(getString(R.string.option_number_format, 1));
                refs.optionLayouts[1].setHint(getString(R.string.option_number_format, 2));
                refs.optionLayouts[2].setHint(getString(R.string.option_number_format, 3));
                refs.optionLayouts[3].setHint(getString(R.string.option_number_format, 4));
                wireQuestionCardListeners(refs, index);
                questionsContainer.addView(card);
                questionCardRefs.add(refs);
            }
        }

        for (int i = 0; i < questions.size(); i++) {
            bindQuestionCard(questionCardRefs.get(i), i, questions.get(i), canRemove);
        }
    }

    private void wireQuestionCardListeners(QuestionCardRefs refs, int index) {
        refs.buttonRemove.setOnClickListener(v -> viewModel.removeQuestion(index));
        refs.toggleQuestionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || refs.suppressWatchers) return;
            viewModel.onQuestionTypeChange(index,
                    checkedId == R.id.buttonQuestionTypeText ? QuestionType.TEXT : QuestionType.MCQ);
        });
        refs.editText.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!refs.suppressWatchers) viewModel.onQuestionTextChange(index, s);
        }));
        for (int i = 0; i < 4; i++) {
            int optionIndex = i;
            refs.optionEdits[i].addTextChangedListener(new SimpleTextWatcher(s -> {
                if (!refs.suppressWatchers) viewModel.onOptionChange(index, optionIndex, s);
            }));
            refs.radios[i].setOnClickListener(v -> viewModel.onCorrectOptionChange(index, optionIndex));
        }
        refs.editMarks.addTextChangedListener(new SimpleTextWatcher(s -> {
            if (!refs.suppressWatchers) viewModel.onMarksChange(index, s);
        }));
    }

    private void bindQuestionCard(QuestionCardRefs refs, int index, QuestionDraft draft, boolean canRemove) {
        refs.textQuestionNumber.setText(getString(R.string.question_number_format, index + 1));
        refs.buttonRemove.setVisibility(canRemove ? View.VISIBLE : View.GONE);

        refs.suppressWatchers = true;
        refs.toggleQuestionType.check(draft.getQuestionType() == QuestionType.TEXT
                ? R.id.buttonQuestionTypeText : R.id.buttonQuestionTypeMcq);
        diffSetText(refs.editText, draft.getText());
        List<String> options = draft.getOptions();
        for (int i = 0; i < 4; i++) {
            diffSetText(refs.optionEdits[i], i < options.size() ? options.get(i) : "");
        }
        diffSetText(refs.editMarks, draft.getMarksText());
        refs.suppressWatchers = false;

        refs.optionsGroup.setVisibility(draft.getQuestionType() == QuestionType.TEXT ? View.GONE : View.VISIBLE);

        Integer correctOptionIndex = draft.getCorrectOptionIndex();
        for (int i = 0; i < 4; i++) {
            refs.radios[i].setChecked(correctOptionIndex != null && correctOptionIndex == i);
        }
    }

    private void diffSetText(TextInputEditText editText, String value) {
        if (!editText.getText().toString().equals(value)) {
            editText.setText(value);
            editText.setSelection(editText.getText().length());
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        CreateQuizUiState current = viewModel.getUiState().getValue();
        Long existing = current != null ? current.getDueDateMillis() : null;
        if (existing != null) calendar.setTimeInMillis(existing);

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            viewModel.onDueDateChange(picked.getTimeInMillis());
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static class QuestionCardRefs {
        final View root;
        final TextView textQuestionNumber;
        final View buttonRemove;
        final MaterialButtonToggleGroup toggleQuestionType;
        final View optionsGroup;
        final TextInputEditText editText;
        final TextInputLayout[] optionLayouts = new TextInputLayout[4];
        final TextInputEditText[] optionEdits = new TextInputEditText[4];
        final RadioButton[] radios = new RadioButton[4];
        final TextInputEditText editMarks;
        boolean suppressWatchers;

        QuestionCardRefs(View root) {
            this.root = root;
            textQuestionNumber = root.findViewById(R.id.textQuestionNumber);
            buttonRemove = root.findViewById(R.id.buttonRemoveQuestion);
            toggleQuestionType = root.findViewById(R.id.toggleQuestionType);
            optionsGroup = root.findViewById(R.id.optionsGroup);
            editText = root.findViewById(R.id.editQuestionText);
            optionLayouts[0] = root.findViewById(R.id.layoutOption1);
            optionLayouts[1] = root.findViewById(R.id.layoutOption2);
            optionLayouts[2] = root.findViewById(R.id.layoutOption3);
            optionLayouts[3] = root.findViewById(R.id.layoutOption4);
            optionEdits[0] = root.findViewById(R.id.editOption1);
            optionEdits[1] = root.findViewById(R.id.editOption2);
            optionEdits[2] = root.findViewById(R.id.editOption3);
            optionEdits[3] = root.findViewById(R.id.editOption4);
            radios[0] = root.findViewById(R.id.radioOption1);
            radios[1] = root.findViewById(R.id.radioOption2);
            radios[2] = root.findViewById(R.id.radioOption3);
            radios[3] = root.findViewById(R.id.radioOption4);
            editMarks = root.findViewById(R.id.editMarks);
        }
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
