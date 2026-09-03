package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class LeaveApplication {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String studentUid = "";
    @Builder.Default
    private final String studentName = "";
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String semesterId = "";
    private final long fromDateMillis;
    private final long toDateMillis;
    @Builder.Default
    private final String reason = "";
    private final String attachmentUrl;
    private final String attachmentName;
    private final String attachmentPublicId;
    private final String attachmentResourceType;
    private final Long attachmentSize;
    @Builder.Default
    private final LeaveStatus status = LeaveStatus.PENDING;
    private final String reviewerUid;
    private final String reviewerName;
    private final String reviewerRole;
    private final Long decidedAt;
    @Builder.Default
    private final long createdAt = 0L;
}
