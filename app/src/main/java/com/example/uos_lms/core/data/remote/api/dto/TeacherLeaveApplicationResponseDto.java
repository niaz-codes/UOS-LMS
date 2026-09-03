package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.LeaveStatus;
import com.example.uos_lms.core.domain.model.LeaveType;
import com.example.uos_lms.core.domain.model.TeacherLeaveApplication;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TeacherLeaveApplicationResponseDto {
    @SerializedName("_id")
    private String id;
    private PersonRefDto teacherId;
    private String departmentId;
    private String leaveType;
    private String fromDate;
    private String toDate;
    private String reason;
    private String status;
    private PersonRefDto reviewerId;
    private String reviewerRole;
    private String decidedAt;
    private String rejectionReason;
    private String createdAt;

    public TeacherLeaveApplication toDomain() {
        return TeacherLeaveApplication.builder()
                .id(id)
                .teacherUid(teacherId != null ? teacherId.getId() : null)
                .teacherName(teacherId != null ? teacherId.getFullName() : null)
                .departmentId(departmentId)
                .leaveType(LeaveType.fromStringOrNull(leaveType))
                .fromDateMillis(IsoDates.toMillis(fromDate))
                .toDateMillis(IsoDates.toMillis(toDate))
                .reason(reason)
                .status(LeaveStatus.fromStringOrNull(status))
                .reviewerUid(reviewerId != null ? reviewerId.getId() : null)
                .reviewerName(reviewerId != null ? reviewerId.getFullName() : null)
                .reviewerRole(reviewerRole)
                .decidedAt(decidedAt != null ? IsoDates.toMillis(decidedAt) : null)
                .rejectionReason(rejectionReason)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
