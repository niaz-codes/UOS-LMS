package com.example.uos_lms.feature.teacher.presentation.quiz;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.domain.model.QuestionType;
import com.example.uos_lms.core.domain.model.QuizQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CreateQuizViewModel extends ViewModel {

    private final ApiQuizDataSource quizDataSource;
    private final String subjectId;

    private final MutableLiveData<CreateQuizUiState> uiState = new MutableLiveData<>(CreateQuizUiState.initial());
    private final MutableLiveData<Boolean> created = new MutableLiveData<>(false);

    @Inject
    public CreateQuizViewModel(
            SavedStateHandle savedStateHandle,
            ApiQuizDataSource quizDataSource) {
        this.quizDataSource = quizDataSource;
        this.subjectId = savedStateHandle.get("subjectId");
    }

    public LiveData<CreateQuizUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getCreated() {
        return created;
    }

    public void onTitleChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().title(value).errorMessage(null).build());
    }

    public void onDescriptionChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().description(value).build());
    }

    public void onTimeLimitChange(String value) {
        uiState.setValue(uiState.getValue().toBuilder().timeLimitText(value).errorMessage(null).build());
    }

    public void onDueDateChange(long millis) {
        uiState.setValue(uiState.getValue().toBuilder().dueDateMillis(millis).errorMessage(null).build());
    }

    public void addQuestion() {
        List<QuestionDraft> questions = new ArrayList<>(uiState.getValue().getQuestions());
        questions.add(QuestionDraft.builder().build());
        uiState.setValue(uiState.getValue().toBuilder().questions(questions).build());
    }

    public void removeQuestion(int index) {
        List<QuestionDraft> questions = new ArrayList<>(uiState.getValue().getQuestions());
        if (questions.size() <= 1) return;
        questions.remove(index);
        uiState.setValue(uiState.getValue().toBuilder().questions(questions).build());
    }

    public void onQuestionTextChange(int index, String value) {
        updateQuestion(index, q -> q.toBuilder().text(value).build());
    }

    public void onQuestionTypeChange(int index, QuestionType type) {
        updateQuestion(index, q -> q.toBuilder().questionType(type).build());
    }

    public void onOptionChange(int index, int optionIndex, String value) {
        updateQuestion(index, q -> {
            List<String> options = new ArrayList<>(q.getOptions());
            options.set(optionIndex, value);
            return q.toBuilder().options(options).build();
        });
    }

    public void onCorrectOptionChange(int index, int optionIndex) {
        updateQuestion(index, q -> q.toBuilder().correctOptionIndex(optionIndex).build());
    }

    public void onMarksChange(int index, String value) {
        updateQuestion(index, q -> q.toBuilder().marksText(value).build());
    }

    private interface QuestionTransform {
        QuestionDraft apply(QuestionDraft draft);
    }

    private void updateQuestion(int index, QuestionTransform transform) {
        List<QuestionDraft> questions = new ArrayList<>(uiState.getValue().getQuestions());
        questions.set(index, transform.apply(questions.get(index)));
        uiState.setValue(uiState.getValue().toBuilder().questions(questions).errorMessage(null).build());
    }

    public void save() {
        CreateQuizUiState state = uiState.getValue();
        Integer timeLimitMinutes = null;
        try {
            timeLimitMinutes = Integer.parseInt(state.getTimeLimitText().trim());
        } catch (NumberFormatException ignored) {
            // handled below via null check
        }
        Long dueDateMillis = state.getDueDateMillis();

        if (state.getTitle().isBlank()) {
            uiState.setValue(state.toBuilder().errorMessage("Title is required").build());
            return;
        }
        if (dueDateMillis == null) {
            uiState.setValue(state.toBuilder().errorMessage("Pick a due date").build());
            return;
        }
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            uiState.setValue(state.toBuilder().errorMessage("Enter a valid time limit in minutes").build());
            return;
        }

        List<QuizQuestion> questions = new ArrayList<>();
        List<QuestionDraft> drafts = state.getQuestions();
        for (int index = 0; index < drafts.size(); index++) {
            QuestionDraft draft = drafts.get(index);
            if (draft.getText().isBlank()) {
                uiState.setValue(state.toBuilder().errorMessage("Question " + (index + 1) + " needs text").build());
                return;
            }
            Integer marks = null;
            try {
                marks = Integer.parseInt(draft.getMarksText().trim());
            } catch (NumberFormatException ignored) {
                // handled below via null check
            }
            if (marks == null || marks <= 0) {
                uiState.setValue(state.toBuilder().errorMessage("Question " + (index + 1) + " needs valid marks").build());
                return;
            }

            if (draft.getQuestionType() == QuestionType.TEXT) {
                questions.add(QuizQuestion.builder()
                        .text(draft.getText().trim())
                        .questionType(QuestionType.TEXT)
                        .options(Collections.emptyList())
                        .correctOptionIndex(-1)
                        .marks(marks)
                        .build());
                continue;
            }

            boolean hasBlankOption = false;
            for (String option : draft.getOptions()) {
                if (option.isBlank()) hasBlankOption = true;
            }
            if (hasBlankOption) {
                uiState.setValue(state.toBuilder().errorMessage("Question " + (index + 1) + " needs all 4 options filled in").build());
                return;
            }
            if (draft.getCorrectOptionIndex() == null) {
                uiState.setValue(state.toBuilder().errorMessage("Question " + (index + 1) + " needs a correct answer selected").build());
                return;
            }

            List<String> trimmedOptions = new ArrayList<>();
            for (String option : draft.getOptions()) trimmedOptions.add(option.trim());

            questions.add(QuizQuestion.builder()
                    .text(draft.getText().trim())
                    .questionType(QuestionType.MCQ)
                    .options(trimmedOptions)
                    .correctOptionIndex(draft.getCorrectOptionIndex())
                    .marks(marks)
                    .build());
        }

        uiState.setValue(state.toBuilder().saving(true).errorMessage(null).build());

        quizDataSource.createQuiz(subjectId, state.getTitle().trim(), state.getDescription().trim(),
                        questions, timeLimitMinutes, dueDateMillis)
                .addOnSuccessListener(v -> {
                    uiState.setValue(uiState.getValue().toBuilder().saving(false).build());
                    created.setValue(true);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder()
                        .saving(false).errorMessage(e.getMessage()).build()));
    }
}
