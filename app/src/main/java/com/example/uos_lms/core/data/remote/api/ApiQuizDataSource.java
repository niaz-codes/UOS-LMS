package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.CreateQuizRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.GradeQuizAttemptRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizAnswerDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizAttemptResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizAttemptsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizQuestionRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.QuizzesEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SaveQuizProgressRequestDto;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAnswer;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.domain.model.QuizQuestion;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreQuizDataSource. One-shot only. */
@Singleton
public class ApiQuizDataSource {

    private final QuizApi quizApi;

    @Inject
    public ApiQuizDataSource(QuizApi quizApi) {
        this.quizApi = quizApi;
    }

    public Task<Quiz> createQuiz(String subjectId, String title, String description,
                                  List<QuizQuestion> questions, int timeLimitMinutes, long dueDateMillis) {
        List<QuizQuestionRequestDto> questionDtos = new ArrayList<>();
        for (QuizQuestion question : questions) {
            questionDtos.add(QuizQuestionRequestDto.builder()
                    .text(question.getText())
                    .questionType(question.getQuestionType().name())
                    .options(question.getOptions())
                    .correctOptionIndex(question.getCorrectOptionIndex())
                    .marks(question.getMarks())
                    .build());
        }
        CreateQuizRequestDto request = CreateQuizRequestDto.builder()
                .subjectId(subjectId).title(title).description(description)
                .questions(questionDtos).timeLimitMinutes(timeLimitMinutes).dueDate(dueDateMillis).build();
        return RetrofitTasks.call(quizApi.create(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getQuiz().toDomain()));
    }

    public Task<Quiz> getQuiz(String quizId) {
        return RetrofitTasks.call(quizApi.getById(quizId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getQuiz().toDomain()));
    }

    /** Admin-only, permanent - also deletes every student attempt against this quiz (see
     * quizController.remove). */
    public Task<Void> deleteQuiz(String quizId) {
        return RetrofitTasks.call(quizApi.remove(quizId));
    }

    public Task<List<Quiz>> quizzesForSubject(String subjectId) {
        return listQuizzes(singleFilter("subjectId", subjectId));
    }

    public Task<List<Quiz>> quizzesForDepartment(String departmentId) {
        return listQuizzes(singleFilter("departmentId", departmentId));
    }

    public Task<List<Quiz>> allQuizzes() {
        return listQuizzes(new HashMap<>());
    }

    private Task<List<Quiz>> listQuizzes(Map<String, String> filters) {
        return RetrofitTasks.call(quizApi.list(filters)).onSuccessTask(envelope -> {
            List<Quiz> quizzes = new ArrayList<>();
            if (envelope.getQuizzes() != null) {
                for (QuizResponseDto dto : envelope.getQuizzes()) quizzes.add(dto.toDomain());
            }
            return Tasks.forResult(quizzes);
        });
    }

    public Task<QuizAttempt> startAttempt(String quizId) {
        return RetrofitTasks.call(quizApi.startAttempt(quizId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getAttempt().toDomain()));
    }

    public Task<QuizAttempt> saveProgress(String attemptId, List<QuizAnswer> answers) {
        List<QuizAnswerDto> answerDtos = new ArrayList<>();
        for (QuizAnswer answer : answers) answerDtos.add(QuizAnswerDto.from(answer));
        SaveQuizProgressRequestDto request = SaveQuizProgressRequestDto.builder().answers(answerDtos).build();
        return RetrofitTasks.call(quizApi.saveProgress(attemptId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getAttempt().toDomain()));
    }

    public Task<QuizAttempt> submitAttempt(String quizId) {
        return RetrofitTasks.call(quizApi.submitAttempt(quizId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getAttempt().toDomain()));
    }

    public Task<QuizAttempt> gradeAttempt(String attemptId, int manualScore, String feedback) {
        GradeQuizAttemptRequestDto request = GradeQuizAttemptRequestDto.builder().manualScore(manualScore).feedback(feedback).build();
        return RetrofitTasks.call(quizApi.gradeAttempt(attemptId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getAttempt().toDomain()));
    }

    public Task<List<QuizAttempt>> attemptsForQuiz(String quizId) {
        return listAttempts(singleFilter("quizId", quizId));
    }

    public Task<List<QuizAttempt>> attemptsForSubject(String subjectId) {
        return listAttempts(singleFilter("subjectId", subjectId));
    }

    /** Student-only: the backend forces studentId to the caller, so this returns 0 or 1 result. */
    public Task<QuizAttempt> myAttemptForQuiz(String quizId) {
        return listAttempts(singleFilter("quizId", quizId)).onSuccessTask(attempts ->
                Tasks.forResult(attempts.isEmpty() ? null : attempts.get(0)));
    }

    private Task<List<QuizAttempt>> listAttempts(Map<String, String> filters) {
        return RetrofitTasks.call(quizApi.listAttempts(filters)).onSuccessTask(this::mapAttempts);
    }

    private Task<List<QuizAttempt>> mapAttempts(QuizAttemptsEnvelopeDto envelope) {
        List<QuizAttempt> attempts = new ArrayList<>();
        if (envelope.getAttempts() != null) {
            for (QuizAttemptResponseDto dto : envelope.getAttempts()) attempts.add(dto.toDomain());
        }
        return Tasks.forResult(attempts);
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }
}
