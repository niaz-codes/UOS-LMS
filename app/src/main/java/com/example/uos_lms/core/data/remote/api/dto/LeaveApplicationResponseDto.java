package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.LeaveApplication;
import com.example.uos_lms.core.domain.model.LeaveStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LeaveApplicationResponseDto {
    @SerializedName("_id")
    private String id;
    private PersonRefDto studentId;
    private String departmentId;
    private String semesterId;
    private String fromDate;
    private String toDate;
    private String reason;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentPublicId;
    private String attachmentResourceType;
    private Long attachmentSize;
    private String status;
    private PersonRefDto reviewerId;
    private String reviewerRole;
    private String decidedAt;
    private String createdAt;

    public LeaveApplication toDomain() {
        return LeaveApplication.builder()
                .id(id)
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : null)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .fromDateMillis(IsoDates.toMillis(fromDate))
                .toDateMillis(IsoDates.toMillis(toDate))
                .reason(reason)
                .attachmentUrl(attachmentUrl)
                .attachmentName(attachmentName)
                .attachmentPublicId(attachmentPublicId)
                .attachmentResourceType(attachmentResourceType)
                .attachmentSize(attachmentSize)
                .status(LeaveStatus.fromStringOrNull(status))
                .reviewerUid(reviewerId != null ? reviewerId.getId() : null)
                .reviewerName(reviewerId != null ? reviewerId.getFullName() : null)
                .reviewerRole(reviewerRole)
                .decidedAt(decidedAt != null ? IsoDates.toMillis(decidedAt) : null)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
