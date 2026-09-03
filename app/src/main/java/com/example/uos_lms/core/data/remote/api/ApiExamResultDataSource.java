package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ExamResultResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamResultRosterRowDto;
import com.example.uos_lms.core.data.remote.api.dto.ExamResultsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.RejectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.StudentMarksDto;
import com.example.uos_lms.core.data.remote.api.dto.StudentSemesterResultResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.StudentSemesterResultsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SubmitResultsRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.SemesterResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SessionResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SubjectResponseDto;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.DepartmentOptions;
import com.example.uos_lms.core.domain.model.ExamResult;
import com.example.uos_lms.core.domain.model.ExamResultRosterRow;
import com.example.uos_lms.core.domain.model.ResultStatus;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Session;
import com.example.uos_lms.core.domain.model.StudentSemesterResultSummary;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreExamResultDataSource. One-shot Task<T> calls
 * (no more real-time listeners - see the migration's locked-in "polling / refresh-on-open"
 * decision); callers refresh explicitly rather than observing a live query. */
@Singleton
public class ApiExamResultDataSource {

    private final GradingApi gradingApi;

    @Inject
    public ApiExamResultDataSource(GradingApi gradingApi) {
        this.gradingApi = gradingApi;
    }

    public Task<List<ExamResult>> saveDrafts(String subjectId, List<StudentMarksDto> marks) {
        return mapResults(RetrofitTasks.call(gradingApi.saveDraft(new SubmitResultsRequestDto(subjectId, marks))));
    }

    public Task<List<ExamResult>> submitForApproval(String subjectId, List<StudentMarksDto> marks) {
        return mapResults(RetrofitTasks.call(gradingApi.submit(new SubmitResultsRequestDto(subjectId, marks))));
    }

    public Task<ExamResult> approve(String resultId) {
        return RetrofitTasks.call(gradingApi.approve(resultId))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getResult().toDomain()));
    }

    public Task<ExamResult> reject(String resultId, String reason) {
        return RetrofitTasks.call(gradingApi.reject(resultId, new RejectRequestDto(reason)))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getResult().toDomain()));
    }

    public Task<List<ExamResult>> listResultsForSubject(String subjectId) {
        return list(singleFilter("subjectId", subjectId));
    }

    /** Admin-only: results a specific teacher has submitted, for the Admin User Profile
     * Overview tab - the backend only honors submittedBy for the ADMIN role. */
    public Task<List<ExamResult>> listBySubmittedBy(String teacherId) {
        return list(singleFilter("submittedBy", teacherId));
    }

    public Task<List<ExamResult>> listRepeatEligibleForSubject(String subjectId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("subjectId", subjectId);
        filters.put("repeatEligible", "true");
        return list(filters);
    }

    /** Role-scoped server-side: HOD sees only their own department. */
    public Task<List<ExamResult>> listPendingApprovals() {
        return list(singleFilter("status", ResultStatus.PENDING_HOD_APPROVAL.name()));
    }

    /** Role-scoped server-side: HOD's "all" tab (own dept), Admin's unfiltered monitor,
     * Student's own APPROVED-only results - the same endpoint, auto-scoped per caller role. */
    public Task<List<ExamResult>> listForCurrentRole() {
        return list(new HashMap<>());
    }

    public Task<List<ExamResult>> listApprovedForStudentInSemester(String studentId, String semesterId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("studentId", studentId);
        filters.put("semesterId", semesterId);
        filters.put("status", ResultStatus.APPROVED.name());
        return list(filters);
    }

    // ---- Dedicated Teacher Exam Result workspace ----

    /** The teacher's authorized departments, each with the sessions (cohorts) and curriculum
     * semesters they actually teach - everything derived server-side from Subject.teacherId. */
    public Task<List<DepartmentOptions>> loadExamResultOptions() {
        return RetrofitTasks.call(gradingApi.examResultOptions()).onSuccessTask(envelope -> {
            List<DepartmentOptions> result = new ArrayList<>();
            if (envelope.getDepartments() != null) {
                for (com.example.uos_lms.core.data.remote.api.dto.ExamResultDepartmentOptionDto dto : envelope.getDepartments()) {
                    List<Session> sessions = new ArrayList<>();
                    for (SessionResponseDto session : dto.getSessions()) sessions.add(session.toDomain());
                    List<Semester> semesters = new ArrayList<>();
                    for (SemesterResponseDto semester : dto.getSemesters()) semesters.add(semester.toDomain());
                    result.add(DepartmentOptions.builder()
                            .department(Department.builder().id(dto.getId()).name(dto.getName()).code(dto.getCode()).build())
                            .sessions(sessions)
                            .semesters(semesters)
                            .build());
                }
            }
            return Tasks.forResult(result);
        });
    }

    /** The teacher's own subjects in a (department, semester) that have students in the
     * chosen session. */
    public Task<List<Subject>> listWorkspaceSubjects(String departmentId, String semesterId, String sessionId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("departmentId", departmentId);
        filters.put("semesterId", semesterId);
        filters.put("sessionId", sessionId);
        return RetrofitTasks.call(gradingApi.examResultSubjects(filters)).onSuccessTask(envelope -> {
            List<Subject> subjects = new ArrayList<>();
            if (envelope.getSubjects() != null) {
                for (SubjectResponseDto dto : envelope.getSubjects()) subjects.add(dto.toDomain());
            }
            return Tasks.forResult(subjects);
        });
    }

    /** Marksheet rows for one subject + optional session filter, per exam type
     * (current | repeat | previous). */
    public Task<List<ExamResultRosterRow>> loadExamResultRoster(String subjectId, String sessionId, String examType) {
        Map<String, String> filters = new HashMap<>();
        filters.put("subjectId", subjectId);
        filters.put("examType", examType);
        if (sessionId != null) filters.put("sessionId", sessionId);
        return RetrofitTasks.call(gradingApi.examResultRoster(filters)).onSuccessTask(envelope -> {
            List<ExamResultRosterRow> rows = new ArrayList<>();
            if (envelope.getRows() != null) {
                for (ExamResultRosterRowDto dto : envelope.getRows()) {
                    rows.add(ExamResultRosterRow.builder()
                            .student(dto.getStudent() != null ? dto.getStudent().toDomain() : null)
                            .result(dto.getResult() != null ? dto.getResult().toDomain() : null)
                            .repeatExam(dto.getRepeatExam() != null ? dto.getRepeatExam().toDomain() : null)
                            .build());
                }
            }
            return Tasks.forResult(rows);
        });
    }

    private Task<List<ExamResult>> list(Map<String, String> filters) {
        return mapResults(RetrofitTasks.call(gradingApi.listResults(filters)));
    }

    /** Results section (separate from the Marks Entry / Approval / Monitor screens above) -
     * backed by StudentSemesterResult, which already carries subject-level detail plus the
     * semester's GPA/CGPA/promotion verdict in one document. Role-scoped server-side: Student
     * is always forced to their own record regardless of filters passed here; HOD is forced to
     * their own department; Admin is unrestricted. Any parameter left null is simply omitted
     * from the query. */
    public Task<List<StudentSemesterResultSummary>> listSemesterResults(
            String departmentId, String sessionId, String semesterId, String studentId) {
        Map<String, String> filters = new HashMap<>();
        if (departmentId != null) filters.put("departmentId", departmentId);
        if (sessionId != null) filters.put("sessionId", sessionId);
        if (semesterId != null) filters.put("semesterId", semesterId);
        if (studentId != null) filters.put("studentId", studentId);
        return RetrofitTasks.call(gradingApi.listSemesterResults(filters))
                .onSuccessTask(envelope -> Tasks.forResult(toSemesterResultDomainList(envelope.getSemesterResults())));
    }

    private static List<StudentSemesterResultSummary> toSemesterResultDomainList(List<StudentSemesterResultResponseDto> dtos) {
        List<StudentSemesterResultSummary> results = new ArrayList<>();
        if (dtos != null) {
            for (StudentSemesterResultResponseDto dto : dtos) results.add(dto.toDomain());
        }
        return results;
    }

    private static Map<String, String> singleFilter(String key, String value) {
        Map<String, String> filters = new HashMap<>();
        filters.put(key, value);
        return filters;
    }

    private static Task<List<ExamResult>> mapResults(Task<ExamResultsEnvelopeDto> task) {
        return task.onSuccessTask(envelope -> Tasks.forResult(toDomainList(envelope.getResults())));
    }

    private static List<ExamResult> toDomainList(List<ExamResultResponseDto> dtos) {
        List<ExamResult> results = new ArrayList<>();
        if (dtos != null) {
            for (ExamResultResponseDto dto : dtos) results.add(dto.toDomain());
        }
        return results;
    }
}
