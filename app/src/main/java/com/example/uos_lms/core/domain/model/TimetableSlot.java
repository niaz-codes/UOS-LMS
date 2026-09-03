package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class TimetableSlot {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String semesterId = "";
    @Builder.Default
    private final String subjectId = "";
    @Builder.Default
    private final String subjectCode = "";
    @Builder.Default
    private final String subjectTitle = "";
    @Builder.Default
    private final String teacherUid = "";
    @Builder.Default
    private final String teacherName = "";
    @Builder.Default
    private final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
    private final int startTimeMinutes;
    private final int endTimeMinutes;
    @Builder.Default
    private final String room = "";
    @Builder.Default
    private final String createdBy = "";
    @Builder.Default
    private final long createdAt = 0L;
}
