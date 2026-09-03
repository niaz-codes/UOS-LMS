package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ExamResult {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String subjectId = "";
    @Builder.Default
    private final String subjectCode = "";
    @Builder.Default
    private final String subjectTitle = "";
    @Builder.Default
    private final int creditHours = 0;
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String semesterId = "";
    @Builder.Default
    private final String teacherUid = "";
    @Builder.Default
    private final String teacherName = "";
    @Builder.Default
    private final String studentUid = "";
    @Builder.Default
    private final String studentName = "";
    private final String studentRollNumber;
    @Builder.Default
    private final int obtainedMarks = 0;
    @Builder.Default
    private final int totalMarks = 100;
    @Builder.Default
    private final ResultStatus status = ResultStatus.DRAFT;
    private final String grade;
    private final Double gpaPoint;
    private final String rejectionReason;
    @Builder.Default
    private final long createdAt = 0L;
    @Builder.Default
    private final long updatedAt = 0L;
    private final Long submittedAt;
    private final String reviewedBy;
    private final Long reviewedAt;

    public double getPercentage() {
        return totalMarks == 0 ? 0.0 : (obtainedMarks * 100.0) / totalMarks;
    }
}
