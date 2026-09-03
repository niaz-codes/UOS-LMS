package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.AssignmentResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentSubmissionResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.AssignmentSubmissionsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.CreateAssignmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.GradeAssignmentRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SubmitAssignmentRequestDto;
import com.example.uos_lms.core.domain.model.Assignment;
import com.example.uos_lms.core.domain.model.AssignmentSubmission;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreAssignmentDataSource. One-shot only. */
@Singleton
public class ApiAssignmentDataSource {

    private final AssignmentApi assignmentApi;

    @Inject
    public ApiAssignmentDataSource(AssignmentApi assignmentApi) {
        this.assignmentApi = assignmentApi;
    }

    public Task<Assignment> createAssignment(String subjectId, String title, String description,
                                              long dueDateMillis, int maxMarks, String mediaId) {
        CreateAssignmentRequestDto request = CreateAssignmentRequestDto.builder()
                .subjectId(subjectId).title(title).description(description)
                .dueDate(dueDateMillis).maxMarks(maxMarks).mediaId(mediaId).build();
        return RetrofitTasks.call(assignmentApi.create(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getAssignment().toDomain()));
    }

    public Task<List<Assignment>> assignmentsForSubject(String subjectId) {
        return listAssignments(singleFilter("subjectId", subjectId));
    }

    public Task<List<Assignment>> assignmentsForDepartment(String departmentId) {
        return listAssignments(singleFilter("departmentId", departmentId));
    }

    public Task<List<Assignment>> allAssignments() {
        return listAssignments(new HashMap<>());
    }

    private Task<List<Assignment>> listAssignments(Map<String, String> filters) {
        return RetrofitTasks.call(assignmentApi.list(filters)).onSuccessTask(envelope -> {
            List<Assignment> assignments = new ArrayList<>();
            if (envelope.getAssignments() != null) {
                for (AssignmentResponseDto dto : envelope.getAssignments()) assignments.add(dto.toDomain());
            }
            return Tasks.forResult(assignments);
        });
    }

    public Task<Void> deleteAssignment(String assignmentId) {
        return RetrofitTasks.call(assignmentApi.remove(assignmentId));
    }

    public Task<AssignmentSubmission> submitAssignment(String assignmentId, String textAnswer, String mediaId) {
        SubmitAssignmentRequestDto request = SubmitAssignmentRequestDto.builder().textAnswer(textAnswer).mediaId(mediaId).build();
        return RetrofitTasks.call(assignmentApi.submit(assignmentId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubmission().toDomain()));
    }

    public Task<AssignmentSubmission> gradeSubmission(String submissionId, int marksObtained, String feedback) {
        GradeAssignmentRequestDto request = GradeAssignmentRequestDto.builder().marksObtained(marksObtained).feedback(feedback).build();
        return RetrofitTasks.call(assignmentApi.grade(submissionId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getSubmission().toDomain()));
    }

    public Task<List<AssignmentSubmission>> submissionsForAssignment(String assignmentId) {
        return listSubmissions(singleFilter("assignmentId", assignmentId));
    }

    public Task<List<AssignmentSubmission>> submissionsForSubject(String subjectId) {
        return listSubmissions(singleFilter("subjectId", subjectId));
    }

    /** Student-only: the backend forces studentId to the caller regardless of filters. */
    public Task<List<AssignmentSubmission>> submissionsForCurrentStudent() {
        return listSubmissions(new HashMap<>());
    }

    public Task<AssignmentSubmission> mySubmissionForAssignment(String assignmentId) {
        return listSubmissions(singleFilter("assignmentId", assignmentId)).onSuccessTask(submissions ->
                Tasks.forResult(submissions.isEmpty() ? null : submissions.get(0)));
    }

    private Task<List<AssignmentSubmission>> listSubmissions(Map<String, String> filters) {
        return RetrofitTasks.call(assignmentApi.listSubmissions(filters)).onSuccessTask(this::mapSubmissions);
    }

    private Task<List<AssignmentSubmission>> mapSubmissions(AssignmentSubmissionsEnvelopeDto envelope) {
        List<AssignmentSubmission> submissions = new ArrayList<>();
        if (envelope.getSubmissions() != null) {
            for (AssignmentSubmissionResponseDto dto : envelope.getSubmissions()) submissions.add(dto.toDomain());
        }
        return Tasks.forResult(submissions);
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }
}
