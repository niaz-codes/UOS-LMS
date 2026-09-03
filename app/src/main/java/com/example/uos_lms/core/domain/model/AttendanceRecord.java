package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AttendanceRecord {
    private final String id;
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String dateKey;
    private final long dateMillis;
    private final String studentUid;
    private final String studentName;
    private final AttendanceStatus status;
    private final String markedBy;
    @Builder.Default
    private final long createdAt = 0L;
}
