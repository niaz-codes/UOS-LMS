package com.example.uos_lms.core.data.remote.api.dto;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** GET /api/grading/exam-result/options - the teacher's authorized Exam Result workspace:
 * departments with their sessions and semesters, all derived server-side from the subjects
 * the teacher owns (never the whole university). */
@Data
@NoArgsConstructor
public class ExamResultOptionsEnvelopeDto {
    private List<ExamResultDepartmentOptionDto> departments;

    public List<ExamResultDepartmentOptionDto> getDepartments() {
        return departments != null ? departments : Collections.emptyList();
    }
}
