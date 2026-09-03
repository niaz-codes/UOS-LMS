package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ExamSchedule {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final ExamType examType = ExamType.MID_TERM;
    @Builder.Default
    private final String subjectId = "";
    @Builder.Default
    private final String subjectCode = "";
    @Builder.Default
    private final String subjectTitle = "";
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String semesterId = "";
    @Builder.Default
    private final String teacherUid = "";
    @Builder.Default
    private final String teacherName = "";
    @Builder.Default
    private final String invigilatorUid = "";
    @Builder.Default
    private final String invigilatorName = "";
    @Builder.Default
    private final String room = "";
    private final long examDateMillis;
    @Builder.Default
    private final String examDateKey = "";
    private final int startTimeMinutes;
    private final int endTimeMinutes;
    @Builder.Default
    private final ExamScheduleStatus status = ExamScheduleStatus.DRAFT;
    @Builder.Default
    private final String createdBy = "";
    @Builder.Default
    private final long createdAt = 0L;
    private final Long publishedAt;
    private final Long lockedAt;
}
