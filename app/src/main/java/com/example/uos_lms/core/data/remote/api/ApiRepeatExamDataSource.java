package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.RejectRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamCreateRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamMarksRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.RepeatExamsEnvelopeDto;
import com.example.uos_lms.core.domain.model.RepeatExam;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ApiRepeatExamDataSource {

    private final GradingApi gradingApi;

    @Inject
    public ApiRepeatExamDataSource(GradingApi gradingApi) {
        this.gradingApi = gradingApi;
    }

    public Task<RepeatExam> create(String examResultId) {
        return RetrofitTasks.call(gradingApi.createRepeatExam(new RepeatExamCreateRequestDto(examResultId)))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getRepeatExam().toDomain()));
    }

    public Task<RepeatExam> submitMarks(String repeatExamId, double marks) {
        return RetrofitTasks.call(gradingApi.submitRepeatExamMarks(repeatExamId, new RepeatExamMarksRequestDto(marks)))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getRepeatExam().toDomain()));
    }

    public Task<RepeatExam> approve(String repeatExamId) {
        return RetrofitTasks.call(gradingApi.approveRepeatExam(repeatExamId))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getRepeatExam().toDomain()));
    }

    public Task<RepeatExam> reject(String repeatExamId, String reason) {
        return RetrofitTasks.call(gradingApi.rejectRepeatExam(repeatExamId, new RejectRequestDto(reason)))
                .onSuccessTask(envelope -> Tasks.forResult(envelope.getRepeatExam().toDomain()));
    }

    /** Teacher: must pass subjectId (own subject only). HOD: department-wide if subjectId
     * omitted. Admin: everything. */
    public Task<List<RepeatExam>> listForSubject(String subjectId) {
        Map<String, String> filters = new HashMap<>();
        filters.put("subjectId", subjectId);
        return list(filters);
    }

    public Task<List<RepeatExam>> listPendingReview() {
        Map<String, String> filters = new HashMap<>();
        filters.put("status", "SUBMITTED");
        return list(filters);
    }

    /** The signed-in Student's own repeat exams, for badging a failed subject row in the
     * Results section - "me" is resolved server-side to the caller's own id. */
    public Task<List<RepeatExam>> listForSelf() {
        return listForStudent("me");
    }

    /** A specific student's repeat exams, for badging a failed subject row when a HOD/Admin
     * drills into that student's semester result. */
    public Task<List<RepeatExam>> listForStudent(String studentId) {
        return RetrofitTasks.call(gradingApi.listRepeatExamsForStudent(studentId)).onSuccessTask(envelope -> {
            List<RepeatExam> repeatExams = new ArrayList<>();
            if (envelope.getRepeatExams() != null) {
                for (RepeatExamResponseDto dto : envelope.getRepeatExams()) repeatExams.add(dto.toDomain());
            }
            return Tasks.forResult(repeatExams);
        });
    }

    private Task<List<RepeatExam>> list(Map<String, String> filters) {
        return RetrofitTasks.call(gradingApi.listRepeatExams(filters)).onSuccessTask(envelope -> {
            List<RepeatExam> repeatExams = new ArrayList<>();
            if (envelope.getRepeatExams() != null) {
                for (RepeatExamResponseDto dto : envelope.getRepeatExams()) repeatExams.add(dto.toDomain());
            }
            return Tasks.forResult(repeatExams);
        });
    }
}
