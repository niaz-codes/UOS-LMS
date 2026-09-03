package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.ExamSchedule;
import com.example.uos_lms.core.domain.model.ExamScheduleStatus;
import com.example.uos_lms.core.domain.model.ExamType;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExamScheduleResponseDto {
    @SerializedName("_id")
    private String id;
    private String examType;
    private SubjectRefDto subjectId;
    private String departmentId;
    private String semesterId;
    private PersonRefDto teacherId;
    private PersonRefDto invigilatorId;
    private String room;
    private String examDate;
    private String examDateKey;
    private int startTimeMinutes;
    private int endTimeMinutes;
    private String status;
    private String createdBy;
    private String createdAt;
    private String publishedAt;
    private String lockedAt;

    public ExamSchedule toDomain() {
        return ExamSchedule.builder()
                .id(id)
                .examType(ExamType.fromStringOrNull(examType))
                .subjectId(subjectId != null ? subjectId.getId() : "")
                .subjectCode(subjectId != null ? subjectId.getCode() : "")
                .subjectTitle(subjectId != null ? subjectId.getTitle() : "")
                .departmentId(departmentId)
                .semesterId(semesterId)
                .teacherUid(teacherId != null ? teacherId.getId() : "")
                .teacherName(teacherId != null ? teacherId.getFullName() : "")
                .invigilatorUid(invigilatorId != null ? invigilatorId.getId() : "")
                .invigilatorName(invigilatorId != null ? invigilatorId.getFullName() : "")
                .room(room)
                .examDateMillis(IsoDates.toMillis(examDate))
                .examDateKey(examDateKey)
                .startTimeMinutes(startTimeMinutes)
                .endTimeMinutes(endTimeMinutes)
                .status(ExamScheduleStatus.fromStringOrNull(status))
                .createdBy(createdBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .publishedAt(publishedAt != null ? IsoDates.toMillis(publishedAt) : null)
                .lockedAt(lockedAt != null ? IsoDates.toMillis(lockedAt) : null)
                .build();
    }
}
