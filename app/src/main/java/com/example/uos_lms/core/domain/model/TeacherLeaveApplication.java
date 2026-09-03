package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class TeacherLeaveApplication {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String teacherUid = "";
    @Builder.Default
    private final String teacherName = "";
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final LeaveType leaveType = LeaveType.OTHER;
    private final long fromDateMillis;
    private final long toDateMillis;
    @Builder.Default
    private final String reason = "";
    @Builder.Default
    private final LeaveStatus status = LeaveStatus.PENDING;
    private final String reviewerUid;
    private final String reviewerName;
    private final String reviewerRole;
    private final Long decidedAt;
    private final String rejectionReason;
    @Builder.Default
    private final long createdAt = 0L;
}
