package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.AttendanceRecord;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AttendanceRecordResponseDto {
    @SerializedName("_id")
    private String id;
    private String subjectId;
    private String departmentId;
    private String semesterId;
    private String dateKey;
    private PersonRefDto studentId;
    private String status;
    private String markedBy;
    private String createdAt;

    public AttendanceRecord toDomain() {
        return AttendanceRecord.builder()
                .id(id)
                .subjectId(subjectId)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .dateKey(dateKey)
                .dateMillis(DateKeyUtils.dateKeyToMillis(dateKey))
                .studentUid(studentId != null ? studentId.getId() : null)
                .studentName(studentId != null ? studentId.getFullName() : "")
                .status(AttendanceStatus.fromStringOrNull(status))
                .markedBy(markedBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
