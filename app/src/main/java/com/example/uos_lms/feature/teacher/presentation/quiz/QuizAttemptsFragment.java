package com.example.uos_lms.feature.teacher.presentation.quiz;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.domain.model.QuizQuestion;
import com.example.uos_lms.core.ui.SimpleListAdapter;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuizAttemptsFragment extends Fragment {

    private QuizAttemptsViewModel viewModel;
    private int totalMarks;

    public QuizAttemptsFragment() {
        super(R.layout.fragment_quiz_attempts);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_attempts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(QuizAttemptsViewModel.class);

        Bundle args = requireArguments();
        String title = args.getString("title");
        totalMarks = args.getInt("totalMarks");

        View toolbar = view.findViewById(R.id.toolbar);
        ((TextView) toolbar.findViewById(R.id.textTitle)).setText(title);
        toolbar.findViewById(R.id.buttonBack).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        View emptyState = view.findViewById(R.id.emptyState);
        ((TextView) emptyState.findViewById(R.id.textEmptyMessage)).setText(R.string.no_attempts_yet);
        RecyclerView recyclerList = view.findViewById(R.id.recyclerList);
        recyclerList.setLayoutManager(new LinearLayoutManager(requireContext()));

        SimpleListAdapter<QuizAttempt> adapter = new SimpleListAdapter<>(R.layout.item_quiz_attempt, (itemView, attempt, position) -> {
            QuizAttemptsUiState state = viewModel.getUiState().getValue();
            boolean needsManualGrading = state != null && state.needsManualGrading();

            ((TextView) itemView.findViewById(R.id.textStudentName)).setText(attempt.getStudentName());
            ((TextView) itemView.findViewById(R.id.textSubmitted)).setText(
                    getString(R.string.submitted_format, DateKeyUtils.millisToDisplay(attempt.getSubmittedAt())));
            ((TextView) itemView.findViewById(R.id.textScore)).setText(
                    getString(R.string.score_out_of_format, attempt.getEffectiveScore(), attempt.getTotalMarks()));

            TextView textFeedback = itemView.findViewById(R.id.textFeedback);
            if (attempt.getFeedback() != null && !attempt.getFeedback().isBlank()) {
                textFeedback.setText(getString(R.string.feedback_prefix_format, attempt.getFeedback()));
                textFeedback.setVisibility(View.VISIBLE);
            } else {
                textFeedback.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> showAttemptDialog(attempt, needsManualGrading));

            com.google.android.material.button.MaterialButton buttonGrade = itemView.findViewById(R.id.buttonGrade);
            if (needsManualGrading) {
                buttonGrade.setVisibility(View.VISIBLE);
                buttonGrade.setText(attempt.isManuallyGraded() ? R.string.update_grade : R.string.grade_button);
                buttonGrade.setOnClickListener(v -> showAttemptDialog(attempt, true));
                com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar),
                        attempt.isManuallyGraded() ? R.color.status_success : R.color.status_warning);
            } else {
                buttonGrade.setVisibility(View.GONE);
                com.example.uos_lms.core.ui.AccentColors.applyBar(itemView.findViewById(R.id.accentBar), R.color.status_success);
            }
        });
        recyclerList.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean hasAttempts = !state.getAttempts().isEmpty();
            emptyState.setVisibility(hasAttempts || state.isLoading() ? View.GONE : View.VISIBLE);
            recyclerList.setVisibility(hasAttempts ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getAttempts());
        });
    }

    /** Always shows the question-by-question breakdown of what the student answered - "Teacher
     * can view student attempts and answers" applies to every quiz, not just gradable ones.
     * The marks/feedback section only shows (and is only editable) when this quiz has a
     * written question that needs a human-assigned score. */
    private void showAttemptDialog(QuizAttempt attempt, boolean gradable) {
        Quiz quiz = viewModel.getUiState().getValue() != null ? viewModel.getUiState().getValue().getQuiz() : null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_grade_attempt, null);
        LinearLayout answersContainer = dialogView.findViewById(R.id.answersContainer);
        View gradingSection = dialogView.findViewById(R.id.gradingSection);
        TextInputLayout layoutMarks = dialogView.findViewById(R.id.layoutMarks);
        TextInputEditText editMarks = dialogView.findViewById(R.id.editMarks);
        TextInputEditText editFeedback = dialogView.findViewById(R.id.editFeedback);
        TextView textError = dialogView.findViewById(R.id.textError);

        if (quiz != null) renderAnswers(answersContainer, quiz, attempt);
        gradingSection.setVisibility(gradable ? View.VISIBLE : View.GONE);

        layoutMarks.setHint(getString(R.string.marks_out_of_format, totalMarks));
        int currentScore = attempt.getManualScore() != null ? attempt.getManualScore() : attempt.getScore();
        editMarks.setText(String.valueOf(currentScore));
        if (attempt.getFeedback() != null) editFeedback.setText(attempt.getFeedback());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(gradable ? R.string.grade_attempt_title : R.string.view_answers_title)
                .setView(dialogView)
                .setNegativeButton(gradable ? R.string.cancel_button : R.string.close_button, null);
        if (gradable) builder.setPositiveButton(R.string.save, null);
        Dialog dialog = builder.show();

        if (!gradable) return;

        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> {
            Integer marks = null;
            try {
                marks = Integer.parseInt(editMarks.getText() == null ? "" : editMarks.getText().toString().trim());
            } catch (NumberFormatException ignored) {
                // handled below via null check
            }
            if (marks == null || marks < 0 || marks > totalMarks) {
                textError.setText(getString(R.string.marks_range_error_format, totalMarks));
                textError.setVisibility(View.VISIBLE);
                return;
            }
            String feedback = editFeedback.getText() == null ? "" : editFeedback.getText().toString().trim();
            viewModel.gradeAttempt(attempt.getId(), marks, feedback);
            dialog.dismiss();
        });
    }

    /** Same read-only rendering approach as TakeQuizFragment.renderResult - MCQ options with
     * the student's pick and the correct answer highlighted, TEXT questions show the raw
     * written answer. */
    private void renderAnswers(LinearLayout container, Quiz quiz, QuizAttempt attempt) {
        container.removeAllViews();
        List<QuizQuestion> questions = quiz.getQuestions();
        List<QuizAnswer> givenAnswers = attempt.getAnswers();
        int primaryColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorPrimary);
        int errorColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorError);
        int onSurfaceColor = MaterialColors.getColor(container, com.google.android.material.R.attr.colorOnSurface);

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            QuizAnswer given = i < givenAnswers.size() ? givenAnswers.get(i) : null;

            View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_take_quiz_question, container, false);
            ((TextView) card.findViewById(R.id.textQuestion)).setText(
                    getString(R.string.question_number_prefix_format, i + 1, question.getText()));
            card.findViewById(R.id.textMarks).setVisibility(View.GONE);

            LinearLayout optionsContainer = card.findViewById(R.id.optionsContainer);

            if (question.isText()) {
                TextView textAnswer = new TextView(requireContext());
                textAnswer.setTextSize(14f);
                textAnswer.setPadding(0, 8, 0, 8);
                String written = given != null && given.getTextAnswer() != null ? given.getTextAnswer().trim() : "";
                textAnswer.setText(written.isEmpty()
                        ? getString(R.string.no_answer_submitted_label)
                        : getString(R.string.your_answer_format, written));
                textAnswer.setTextColor(onSurfaceColor);
                optionsContainer.addView(textAnswer);
                container.addView(card);
                continue;
            }

            List<String> options = question.getOptions();
            Integer givenOptionIndex = given != null ? given.getOptionIndex() : null;
            for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                boolean isCorrect = optionIndex == question.getCorrectOptionIndex();
                boolean isGivenAnswer = givenOptionIndex != null && optionIndex == givenOptionIndex;

                TextView textOption = new TextView(requireContext());
                textOption.setTextSize(14f);
                textOption.setPadding(0, 8, 0, 8);
                String option = options.get(optionIndex);
                if (isCorrect && isGivenAnswer) {
                    textOption.setText(getString(R.string.your_answer_correct_format, option));
                    textOption.setTextColor(primaryColor);
                } else if (isCorrect) {
                    textOption.setText(getString(R.string.correct_answer_format, option));
                    textOption.setTextColor(primaryColor);
                } else if (isGivenAnswer) {
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
}
