package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** One subject's frozen result within a StudentSemesterResult - matches
 * backend/src/models/StudentSemesterResult.js's subjectResultSnapshotSchema. */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SubjectResultSnapshot {
    @Builder.Default
    private final String subjectId = "";
    @Builder.Default
    private final String courseCode = "";
    @Builder.Default
    private final String subjectName = "";
    @Builder.Default
    private final int creditHours = 0;
    @Builder.Default
    private final double marks = 0.0;
    @Builder.Default
    private final String grade = "";
    @Builder.Default
    private final double gpa = 0.0;
    private final SubjectResultStatus status;
}
