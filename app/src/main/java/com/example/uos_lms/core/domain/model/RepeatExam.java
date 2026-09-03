package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RepeatExam {
    private final String id;
    private final String examResultId;

    private final String studentUid;
    private final String studentName;
    private final String studentRollNumber;

    private final String subjectId;
    private final String subjectCode;
    private final String subjectTitle;

    private final double previousMarks;
    private final String previousGrade;
    private final double previousGpa;

    private final Double newMarks;
    private final String newGrade;
    private final Double newGpa;

    private final RepeatStatus repeatStatus;
    private final String rejectionReason;

    private final long createdAt;

    public boolean isEditable() {
        return repeatStatus == RepeatStatus.PENDING || repeatStatus == RepeatStatus.REJECTED;
    }
}
