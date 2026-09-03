package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/** The read-optimized per-(student, semester) rollup powering the Results section - mirrors
 * backend/src/models/StudentSemesterResult.js. Unlike the flat ExamResult list, this already
 * carries subject-level detail plus the semester-wide GPA/CGPA/promotion verdict in one
 * document, computed by recalculateSemesterGpa on every result approval. */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class StudentSemesterResultSummary {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String studentUid = "";
    @Builder.Default
    private final String studentName = "";
    private final String studentRollNumber;
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String semesterId = "";
    @Builder.Default
    private final int semesterNumber = 0;
    @Builder.Default
    private final String sessionId = "";
    @Builder.Default
    private final String sessionLabel = "";
    @Builder.Default
    private final List<SubjectResultSnapshot> subjectResults = Collections.emptyList();
    @Builder.Default
    private final double semesterGpa = 0.0;
    private final Double previousSemesterGpa;
    @Builder.Default
    private final double cumulativeCgpa = 0.0;
    @Builder.Default
    private final ResultStatus resultStatus = ResultStatus.DRAFT;
    @Builder.Default
    private final PromotionStatus promotionStatus = PromotionStatus.NOT_EVALUATED;
    @Builder.Default
    private final List<String> failedSubjectIds = Collections.emptyList();
    @Builder.Default
    private final long createdAt = 0L;
    @Builder.Default
    private final long updatedAt = 0L;

    public String getSemesterLabel() {
        return "Semester " + semesterNumber;
    }

    public int getTotalSubjectCount() {
        return subjectResults.size();
    }

    public int getPassedSubjectCount() {
        int count = 0;
        for (SubjectResultSnapshot subject : subjectResults) {
            if (subject.getStatus() == SubjectResultStatus.PASS) count++;
        }
        return count;
    }

    public int getFailedSubjectCount() {
        int count = 0;
        for (SubjectResultSnapshot subject : subjectResults) {
            if (subject.getStatus() == SubjectResultStatus.FAIL) count++;
        }
        return count;
    }

    public int getTotalCreditHours() {
        int total = 0;
        for (SubjectResultSnapshot subject : subjectResults) total += subject.getCreditHours();
        return total;
    }
}
