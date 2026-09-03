package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.common.DateKeyUtils;
import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.AttendanceStatus;
import com.example.uos_lms.core.domain.model.TeacherAttendanceRecord;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TeacherAttendanceRecordResponseDto {
    @SerializedName("_id")
    private String id;
    private String departmentId;
    private String semesterId;
    private SubjectRefDto subjectId;
    private String dateKey;
    private PersonRefDto teacherId;
    private String status;
    private String markedBy;
    private String createdAt;

    public TeacherAttendanceRecord toDomain() {
        return TeacherAttendanceRecord.builder()
                .id(id)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .subjectId(subjectId != null ? subjectId.getId() : null)
                .subjectCode(subjectId != null ? subjectId.getCode() : "")
                .subjectTitle(subjectId != null ? subjectId.getTitle() : "")
                .dateKey(dateKey)
                .dateMillis(DateKeyUtils.dateKeyToMillis(dateKey))
                .teacherUid(teacherId != null ? teacherId.getId() : null)
                .teacherName(teacherId != null ? teacherId.getFullName() : "")
                .status(AttendanceStatus.fromStringOrNull(status))
                .markedBy(markedBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
