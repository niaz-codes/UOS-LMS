package com.example.uos_lms.feature.student.presentation.quiz;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TakeQuizViewModel extends ViewModel {

    private static final long TEXT_AUTOSAVE_DEBOUNCE_MS = 1500L;

    private final ApiQuizDataSource quizDataSource;
    private final String quizId;

    private final MutableLiveData<TakeQuizUiState> uiState = new MutableLiveData<>(TakeQuizUiState.initial());

    private CountDownTimer timer;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSave;

    @Inject
    public TakeQuizViewModel(
            SavedStateHandle savedStateHandle,
            ApiQuizDataSource quizDataSource) {
        this.quizDataSource = quizDataSource;
        this.quizId = savedStateHandle.get("quizId");
        load();
    }

    public LiveData<TakeQuizUiState> getUiState() {
        return uiState;
    }

    private void load() {
        quizDataSource.getQuiz(quizId)
                .addOnSuccessListener(quiz -> {
                    uiState.setValue(uiState.getValue().toBuilder().quiz(quiz).build());
                    loadAttempt(quiz);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private void loadAttempt(Quiz quiz) {
        quizDataSource.myAttemptForQuiz(quizId)
                .addOnSuccessListener(attempt -> {
                    List<QuizAnswer> answers = hydrateAnswers(quiz, attempt);
                    uiState.setValue(uiState.getValue().toBuilder()
                            .existingAttempt(attempt).answers(answers).loading(false).build());
                    if (attempt != null && !attempt.isSubmitted()) {
                        resumeTimer(quiz, attempt);
                    }
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).errorMessage(e.getMessage()).build()));
    }

    private static List<QuizAnswer> hydrateAnswers(Quiz quiz, QuizAttempt attempt) {
        int size = quiz.getQuestions().size();
        List<QuizAnswer> source = attempt != null ? attempt.getAnswers() : null;
        List<QuizAnswer> answers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            QuizAnswer existing = source != null && i < source.size() ? source.get(i) : null;
            answers.add(existing != null ? existing : QuizAnswer.empty());
        }
        return answers;
    }

    /** Explicit first step, matching the spec's "Start Quiz" - this is what calls the
     * backend's /start endpoint and records the server-anchored startedAt the countdown is
     * built from (see resumeTimer). */
    public void startQuiz() {
        TakeQuizUiState state = uiState.getValue();
        if (state.getQuiz() == null || state.isStarting() || state.getExistingAttempt() != null) return;

        uiState.setValue(state.toBuilder().starting(true).errorMessage(null).build());
        quizDataSource.startAttempt(quizId)
                .addOnSuccessListener(attempt -> {
                    Quiz quiz = uiState.getValue().getQuiz();
                    List<QuizAnswer> answers = hydrateAnswers(quiz, attempt);
                    uiState.setValue(uiState.getValue().toBuilder()
                            .starting(false).existingAttempt(attempt).answers(answers).build());
                    resumeTimer(quiz, attempt);
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().starting(false).errorMessage(e.getMessage()).build()));
    }

    /** Anchors the countdown to the server-recorded startedAt, not a fresh full duration each
     * time - this is what makes closing and reopening the app not reset the timer back to
     * full time (the bug the old client-only timer had). If the deadline has already passed
     * (app was closed past the time limit), finalizes immediately instead of showing a timer. */
    private void resumeTimer(Quiz quiz, QuizAttempt attempt) {
        if (timer != null) timer.cancel();

        long totalMillis = quiz.getTimeLimitMinutes() * 60_000L;
        long elapsed = System.currentTimeMillis() - attempt.getStartedAt();
        long remaining = totalMillis - elapsed;

        if (remaining <= 0) {
            uiState.setValue(uiState.getValue().toBuilder().secondsRemaining(0).build());
            submit();
            return;
        }

        uiState.setValue(uiState.getValue().toBuilder().secondsRemaining((int) (remaining / 1000)).build());
        timer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                uiState.setValue(uiState.getValue().toBuilder().secondsRemaining((int) (millisUntilFinished / 1000)).build());
            }

            @Override
            public void onFinish() {
                uiState.setValue(uiState.getValue().toBuilder().secondsRemaining(0).build());
                submit();
            }
        };
        timer.start();
    }

    public void selectAnswer(int questionIndex, int optionIndex) {
        updateAnswer(questionIndex, QuizAnswer.builder().optionIndex(optionIndex).build());
        flushSave();
    }

    public void updateTextAnswer(int questionIndex, String text) {
        updateAnswer(questionIndex, QuizAnswer.builder().textAnswer(text).build());
        debounceSave();
    }

    private void updateAnswer(int questionIndex, QuizAnswer answer) {
        List<QuizAnswer> answers = new ArrayList<>(uiState.getValue().getAnswers());
        if (questionIndex < 0 || questionIndex >= answers.size()) return;
        answers.set(questionIndex, answer);
        uiState.setValue(uiState.getValue().toBuilder().answers(answers).build());
    }

    /** Text answers autosave on a short debounce (not on every keystroke) - MCQ picks save
     * immediately since they're infrequent, discrete events. */
    private void debounceSave() {
        if (pendingSave != null) debounceHandler.removeCallbacks(pendingSave);
        pendingSave = this::flushSave;
        debounceHandler.postDelayed(pendingSave, TEXT_AUTOSAVE_DEBOUNCE_MS);
    }

    private void flushSave() {
        if (pendingSave != null) {
            debounceHandler.removeCallbacks(pendingSave);
            pendingSave = null;
        }
        TakeQuizUiState state = uiState.getValue();
        QuizAttempt attempt = state.getExistingAttempt();
        if (attempt == null || attempt.isSubmitted()) return;
        quizDataSource.saveProgress(attempt.getId(), state.getAnswers());
    }

    public void submit() {
        TakeQuizUiState state = uiState.getValue();
        if (state.getQuiz() == null || state.getExistingAttempt() == null) return;
        if (state.isSubmitting() || state.getExistingAttempt().isSubmitted()) return;
        if (timer != null) timer.cancel();
        if (pendingSave != null) {
            debounceHandler.removeCallbacks(pendingSave);
            pendingSave = null;
        }

        uiState.setValue(state.toBuilder().submitting(true).errorMessage(null).build());

        // Flush the latest on-screen answers first, then finalize - submitAttempt trusts only
        // what's already persisted server-side, never a client-supplied answer set, so this
        // save has to land before submit for the very last edit to count.
        quizDataSource.saveProgress(state.getExistingAttempt().getId(), state.getAnswers())
                .continueWithTask(ignored -> quizDataSource.submitAttempt(quizId))
                .addOnSuccessListener(attempt -> {
                    uiState.setValue(uiState.getValue().toBuilder().submitting(false).existingAttempt(attempt).build());
                    // Re-fetch: the backend only reveals correctOptionIndex once submitted, so
                    // the review screen needs the now-unlocked quiz, not the pre-submit one.
                    quizDataSource.getQuiz(quizId).addOnSuccessListener(quiz ->
                            uiState.setValue(uiState.getValue().toBuilder().quiz(quiz).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().submitting(false).errorMessage(e.getMessage()).build()));
    }

    @Override
    protected void onCleared() {
        if (timer != null) timer.cancel();
        if (pendingSave != null) debounceHandler.removeCallbacks(pendingSave);
    }
}
