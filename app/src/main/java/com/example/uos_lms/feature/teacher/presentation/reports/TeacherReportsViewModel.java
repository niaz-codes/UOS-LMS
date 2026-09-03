package com.example.uos_lms.feature.teacher.presentation.reports;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.uos_lms.core.data.remote.api.ApiAssignmentDataSource;
import com.example.uos_lms.core.data.remote.api.ApiAttendanceDataSource;
import com.example.uos_lms.core.data.remote.api.ApiQuizDataSource;
import com.example.uos_lms.core.data.remote.api.ApiUniversityDataSource;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.RetrofitTasks;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.Quiz;
import com.example.uos_lms.core.domain.model.QuizAttempt;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.User;
import com.example.uos_lms.core.ui.ChartEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TeacherReportsViewModel extends ViewModel {

    private final MutableLiveData<TeacherReportsUiState> uiState = new MutableLiveData<>(TeacherReportsUiState.initial());

    private List<Subject> latestSubjects;
    private final Map<String, List<Assignment>> assignmentsBySubject = new HashMap<>();
    private final Map<String, List<Quiz>> quizzesBySubject = new HashMap<>();
    private final Map<String, List<AttendanceRecord>> attendanceBySubjectId = new HashMap<>();
    private final Map<String, List<AssignmentSubmission>> submissionsBySubject = new HashMap<>();
    private final Map<String, List<QuizAttempt>> attemptsBySubject = new HashMap<>();
    private final Map<String, List<User>> studentsBySubject = new HashMap<>();

    @Inject
    public TeacherReportsViewModel(
            ApiUniversityDataSource universityDataSource,
            ApiAttendanceDataSource attendanceDataSource,
            ApiAssignmentDataSource assignmentDataSource,
            ApiQuizDataSource quizDataSource,
            AuthApi authApi) {
        RetrofitTasks.call(authApi.me())
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getUser().toDomain()))
                .addOnSuccessListener(user -> {
                    if (user == null) {
                        uiState.setValue(uiState.getValue().toBuilder().loading(false).build());
                        return;
                    }
                    universityDataSource.listSubjectsForTeacher(user.getUid()).addOnSuccessListener(subjects -> {
                        latestSubjects = subjects;
                        loadAll(subjects, universityDataSource, attendanceDataSource, assignmentDataSource, quizDataSource);
                    }).addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
                })
                .addOnFailureListener(e -> uiState.setValue(uiState.getValue().toBuilder().loading(false).build()));
    }

    public LiveData<TeacherReportsUiState> getUiState() {
        return uiState;
    }

    private void loadAll(List<Subject> subjects, ApiUniversityDataSource universityDataSource,
                          ApiAttendanceDataSource attendanceDataSource, ApiAssignmentDataSource assignmentDataSource,
                          ApiQuizDataSource quizDataSource) {
        List<Task<?>> tasks = new ArrayList<>();
        for (Subject subject : subjects) {
            String id = subject.getId();
            tasks.add(assignmentDataSource.assignmentsForSubject(id).addOnSuccessListener(v -> assignmentsBySubject.put(id, v)));
            tasks.add(quizDataSource.quizzesForSubject(id).addOnSuccessListener(v -> quizzesBySubject.put(id, v)));
            tasks.add(attendanceDataSource.historyForSubject(id).addOnSuccessListener(v -> attendanceBySubjectId.put(id, v)));
            tasks.add(assignmentDataSource.submissionsForSubject(id).addOnSuccessListener(v -> submissionsBySubject.put(id, v)));
            tasks.add(quizDataSource.attemptsForSubject(id).addOnSuccessListener(v -> attemptsBySubject.put(id, v)));
            tasks.add(universityDataSource.subjectRoster(id).addOnSuccessListener(v -> studentsBySubject.put(id, v)));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> recompute());
    }

    private void recompute() {
        if (latestSubjects == null) return;

        Map<String, User> distinctStudents = new LinkedHashMap<>();
        for (List<User> students : studentsBySubject.values()) {
            for (User student : students) distinctStudents.put(student.getUid(), student);
        }

        int totalAssignments = 0;
        int totalQuizzes = 0;
        List<ChartEntry> attendanceBySubject = new ArrayList<>();
        List<ChartEntry> performanceBySubject = new ArrayList<>();

        for (Subject subject : latestSubjects) {
            List<Assignment> assignments = assignmentsBySubject.getOrDefault(subject.getId(), new ArrayList<>());
            List<Quiz> quizzes = quizzesBySubject.getOrDefault(subject.getId(), new ArrayList<>());
            totalAssignments += assignments.size();
            totalQuizzes += quizzes.size();

            List<AttendanceRecord> records = attendanceBySubjectId.getOrDefault(subject.getId(), new ArrayList<>());
            int present = 0;
            for (AttendanceRecord record : records) {
                if (record.getStatus() == AttendanceStatus.PRESENT) present++;
            }
            int attendancePercentage = records.isEmpty() ? 0 : (present * 100) / records.size();
            attendanceBySubject.add(new ChartEntry(subject.getCode(), attendancePercentage));

            Map<String, Assignment> assignmentById = new HashMap<>();
            for (Assignment assignment : assignments) assignmentById.put(assignment.getId(), assignment);

            List<Integer> percentages = new ArrayList<>();
            List<AssignmentSubmission> submissions = submissionsBySubject.getOrDefault(subject.getId(), new ArrayList<>());
            for (AssignmentSubmission submission : submissions) {
                if (!submission.getSubjectId().equals(subject.getId()) || !submission.isGraded()) continue;
                Assignment assignment = assignmentById.get(submission.getAssignmentId());
                if (assignment == null || assignment.getMaxMarks() <= 0) continue;
                percentages.add((submission.getMarksObtained() * 100) / assignment.getMaxMarks());
            }
            List<QuizAttempt> attempts = attemptsBySubject.getOrDefault(subject.getId(), new ArrayList<>());
            for (QuizAttempt attempt : attempts) {
                if (!attempt.getSubjectId().equals(subject.getId()) || attempt.getTotalMarks() <= 0) continue;
                percentages.add((attempt.getEffectiveScore() * 100) / attempt.getTotalMarks());
            }
            int performancePercentage = 0;
            if (!percentages.isEmpty()) {
                int sum = 0;
                for (int p : percentages) sum += p;
                performancePercentage = sum / percentages.size();
            }
            performanceBySubject.add(new ChartEntry(subject.getCode(), performancePercentage));
        }

        uiState.setValue(uiState.getValue().toBuilder()
                .totalSubjects(latestSubjects.size())
                .totalStudents(distinctStudents.size())
                .totalAssignments(totalAssignments)
                .totalQuizzes(totalQuizzes)
                .attendanceBySubject(attendanceBySubject)
                .performanceBySubject(performanceBySubject)
                .loading(false)
                .build());
    }
}
