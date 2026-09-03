package com.example.uos_lms.feature.student.presentation.quiz;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.uos_lms.R;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.domain.model.QuizQuestion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TakeQuizFragment extends Fragment {

    private TakeQuizViewModel viewModel;
    /** Parallel to the quiz's questions - null at any index whose question is TEXT (only MCQ
     * cards need their radio-checked state re-synced on every render). */
    private final List<RadioButton[]> questionRadios = new ArrayList<>();
    private boolean formBuilt;
    private boolean resultBuilt;

    public TakeQuizFragment() {
        super(R.layout.fragment_take_quiz);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_take_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TakeQuizViewModel.class);

        View toolbar = view.findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        view.findViewById(R.id.buttonSubmit).setOnClickListener(v -> viewModel.submit());
        view.findViewById(R.id.buttonStartQuiz).setOnClickListener(v -> viewModel.startQuiz());

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, TakeQuizUiState state) {
        ((TextView) view.findViewById(R.id.toolbar).findViewById(R.id.textTitle)).setText(
                state.getQuiz() != null ? state.getQuiz().getTitle() : getString(R.string.quiz_fallback_title));

        ProgressBar progressLoading = view.findViewById(R.id.progressLoading);
        View emptyState = view.findViewById(R.id.emptyState);
        View preStartContainer = view.findViewById(R.id.preStartContainer);
        View formContainer = view.findViewById(R.id.formContainer);
        View resultContainer = view.findViewById(R.id.resultContainer);

        Quiz quiz = state.getQuiz();
        QuizAttempt attempt = state.getExistingAttempt();
        boolean isClosed = attempt == null && quiz != null && System.currentTimeMillis() > quiz.getDueDateMillis();

        progressLoading.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        if (state.isLoading()) {
            emptyState.setVisibility(View.GONE);
            preStartContainer.setVisibility(View.GONE);
            formContainer.setVisibility(View.GONE);
            resultContainer.setVisibility(View.GONE);
            return;
        }

        if (quiz == null) {
            emptyState.setVisibility(View.VISIBLE);
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(
                    state.getErrorMessage() != null ? state.getErrorMessage() : getString(R.string.quiz_not_found_message));
            preStartContainer.setVisibility(View.GONE);
            formContainer.setVisibility(View.GONE);
            resultContainer.setVisibility(View.GONE);
            return;
        }

        if (attempt != null && attempt.isSubmitted()) {
            emptyState.setVisibility(View.GONE);
            preStartContainer.setVisibility(View.GONE);
            formContainer.setVisibility(View.GONE);
            resultContainer.setVisibility(View.VISIBLE);
            renderResult(view, quiz, attempt);
            return;
        }

        if (attempt != null) {
            emptyState.setVisibility(View.GONE);
            preStartContainer.setVisibility(View.GONE);
            formContainer.setVisibility(View.VISIBLE);
            resultContainer.setVisibility(View.GONE);
            renderForm(view, quiz, state);
            return;
        }

        if (isClosed) {
            emptyState.setVisibility(View.VISIBLE);
            ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.quiz_closed_message);
            preStartContainer.setVisibility(View.GONE);
            formContainer.setVisibility(View.GONE);
            resultContainer.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        preStartContainer.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        renderPreStart(view, quiz, state);
    }

    private void renderPreStart(View view, Quiz quiz, TakeQuizUiState state) {
        ((TextView) view.findViewById(R.id.textPreStartTitle)).setText(quiz.getTitle());

        TextView textDescription = view.findViewById(R.id.textPreStartDescription);
        if (quiz.getDescription() != null && !quiz.getDescription().isBlank()) {
            textDescription.setText(quiz.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        } else {
            textDescription.setVisibility(View.GONE);
        }

        ((TextView) view.findViewById(R.id.textPreStartMeta)).setText(getString(R.string.quiz_prestart_meta_format,
                quiz.getQuestions().size(), quiz.getTotalMarks(), quiz.getTimeLimitMinutes()));

        TextView textError = view.findViewById(R.id.textPreStartError);
        if (state.getErrorMessage() != null) {
            textError.setText(state.getErrorMessage());
            textError.setVisibility(View.VISIBLE);
        } else {
            textError.setVisibility(View.GONE);
        }

        boolean starting = state.isStarting();
        MaterialButton buttonStart = view.findViewById(R.id.buttonStartQuiz);
        buttonStart.setEnabled(!starting);
        view.findViewById(R.id.progressStarting).setVisibility(starting ? View.VISIBLE : View.GONE);
    }

    private void renderForm(View view, Quiz quiz, TakeQuizUiState state) {
        ((TextView) view.findViewById(R.id.textTimeRemaining)).setText(
                getString(R.string.time_remaining_format, formatSeconds(state.getSecondsRemaining())));

        LinearLayout container = view.findViewById(R.id.formQuestionsContainer);
        List<QuizQuestion> questions = quiz.getQuestions();
        if (!formBuilt) {
            formBuilt = true;
            container.removeAllViews();
            questionRadios.clear();
            List<QuizAnswer> initialAnswers = state.getAnswers();

            for (int i = 0; i < questions.size(); i++) {
                int questionIndex = i;
                QuizQuestion question = questions.get(i);
                QuizAnswer initial = questionIndex < initialAnswers.size() ? initialAnswers.get(questionIndex) : null;

                if (question.isText()) {
                    View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_take_quiz_text_question, container, false);
                    ((TextView) card.findViewById(R.id.textQuestion)).setText(
                            getString(R.string.question_number_prefix_format, questionIndex + 1, question.getText()));
                    ((TextView) card.findViewById(R.id.textMarks)).setText(getString(R.string.marks_count_format, question.getMarks()));
                    TextInputEditText editAnswer = card.findViewById(R.id.editTextAnswer);
                    if (initial != null && initial.getTextAnswer() != null) {
                        editAnswer.setText(initial.getTextAnswer());
                    }
                    editAnswer.addTextChangedListener(new SimpleTextWatcher(text -> viewModel.updateTextAnswer(questionIndex, text)));
                    questionRadios.add(null);
                    container.addView(card);
                    continue;
                }

                View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_take_quiz_question, container, false);
                ((TextView) card.findViewById(R.id.textQuestion)).setText(
                        getString(R.string.question_number_prefix_format, questionIndex + 1, question.getText()));
                ((TextView) card.findViewById(R.id.textMarks)).setText(getString(R.string.marks_count_format, question.getMarks()));

                LinearLayout optionsContainer = card.findViewById(R.id.optionsContainer);
                RadioButton[] radios = new RadioButton[question.getOptions().size()];
                for (int optionIndex = 0; optionIndex < question.getOptions().size(); optionIndex++) {
                    View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_take_quiz_option_row, optionsContainer, false);
                    RadioButton radio = row.findViewById(R.id.radioOption);
                    TextView textOption = row.findViewById(R.id.textOption);
                    textOption.setText(question.getOptions().get(optionIndex));
                    int finalOptionIndex = optionIndex;
                    row.setOnClickListener(v -> viewModel.selectAnswer(questionIndex, finalOptionIndex));
                    radios[optionIndex] = radio;
                    optionsContainer.addView(row);
                }
                questionRadios.add(radios);
                container.addView(card);
            }
        }

        List<QuizAnswer> answers = state.getAnswers();
        for (int i = 0; i < questionRadios.size(); i++) {
            RadioButton[] radios = questionRadios.get(i);
            if (radios == null) continue;
            Integer selected = i < answers.size() && answers.get(i) != null ? answers.get(i).getOptionIndex() : null;
            for (int optionIndex = 0; optionIndex < radios.length; optionIndex++) {
                radios[optionIndex].setChecked(selected != null && selected == optionIndex);
            }
        }

        TextView textError = view.findViewById(R.id.textError);
        if (state.getErrorMessage() != null) {
            textError.setText(state.getErrorMessage());
            textError.setVisibility(View.VISIBLE);
        } else {
            textError.setVisibility(View.GONE);
        }

        boolean submitting = state.isSubmitting();
        MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);
        buttonSubmit.setEnabled(!submitting);
        CircularProgressIndicator progressSubmitting = view.findViewById(R.id.progressSubmitting);
        progressSubmitting.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    private void renderResult(View view, Quiz quiz, QuizAttempt attempt) {
        ((TextView) view.findViewById(R.id.textResultScore)).setText(
                getString(R.string.score_out_of_format, attempt.getEffectiveScore(), attempt.getTotalMarks()));

        boolean hasTextQuestions = quiz.getQuestions().stream().anyMatch(QuizQuestion::isText);
        view.findViewById(R.id.textPendingGrading).setVisibility(
                hasTextQuestions && !attempt.isManuallyGraded() ? View.VISIBLE : View.GONE);

        TextView textFeedback = view.findViewById(R.id.textResultFeedback);
        if (attempt.getFeedback() != null && !attempt.getFeedback().isBlank()) {
            textFeedback.setText(getString(R.string.feedback_prefix_format, attempt.getFeedback()));
            textFeedback.setVisibility(View.VISIBLE);
        } else {
            textFeedback.setVisibility(View.GONE);
        }

        if (resultBuilt) return;
        resultBuilt = true;

        LinearLayout container = view.findViewById(R.id.resultQuestionsContainer);
        container.removeAllViews();
        List<QuizQuestion> questions = quiz.getQuestions();
        List<QuizAnswer> yourAnswers = attempt.getAnswers();
        int primaryColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorPrimary);
        int errorColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorError);
        int onSurfaceColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorOnSurface);

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            QuizAnswer yourAnswer = i < yourAnswers.size() ? yourAnswers.get(i) : null;

            View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_take_quiz_question, container, false);
            ((TextView) card.findViewById(R.id.textQuestion)).setText(
                    getString(R.string.question_number_prefix_format, i + 1, question.getText()));
            card.findViewById(R.id.textMarks).setVisibility(View.GONE);

            LinearLayout optionsContainer = card.findViewById(R.id.optionsContainer);

            if (question.isText()) {
                TextView textAnswer = new TextView(requireContext());
                textAnswer.setTextSize(14f);
                textAnswer.setPadding(0, 8, 0, 8);
                String written = yourAnswer != null && yourAnswer.getTextAnswer() != null ? yourAnswer.getTextAnswer().trim() : "";
                textAnswer.setText(written.isEmpty()
                        ? getString(R.string.no_answer_submitted_label)
                        : getString(R.string.your_answer_format, written));
                textAnswer.setTextColor(onSurfaceColor);
                optionsContainer.addView(textAnswer);
                container.addView(card);
                continue;
            }

            List<String> options = question.getOptions();
            Integer yourOptionIndex = yourAnswer != null ? yourAnswer.getOptionIndex() : null;
            for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                boolean isCorrect = optionIndex == question.getCorrectOptionIndex();
                boolean isYourAnswer = yourOptionIndex != null && optionIndex == yourOptionIndex;

                TextView textOption = new TextView(requireContext());
                textOption.setTextSize(14f);
                textOption.setPadding(0, 8, 0, 8);
                String option = options.get(optionIndex);
                if (isCorrect && isYourAnswer) {
                    textOption.setText(getString(R.string.your_answer_correct_format, option));
                    textOption.setTextColor(primaryColor);
                } else if (isCorrect) {
                    textOption.setText(getString(R.string.correct_answer_format, option));
                    textOption.setTextColor(primaryColor);
                } else if (isYourAnswer) {
                    textOption.setText(getString(R.string.your_answer_incorrect_format, option));
                    textOption.setTextColor(errorColor);
                } else {
                    textOption.setText(option);
                    textOption.setTextColor(onSurfaceColor);
                }
                optionsContainer.addView(textOption);
            }
            container.addView(card);
        }
    }

    private String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
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
