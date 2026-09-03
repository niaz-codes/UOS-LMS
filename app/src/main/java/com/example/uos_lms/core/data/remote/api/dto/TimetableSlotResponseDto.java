package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.TimetableSlot;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimetableSlotResponseDto {
    @SerializedName("_id")
    private String id;
    private String departmentId;
    private String semesterId;
    private SubjectRefDto subjectId;
    private PersonRefDto teacherId;
    private String dayOfWeek;
    private int startTimeMinutes;
    private int endTimeMinutes;
    private String room;
    private String createdBy;
    private String createdAt;

    public TimetableSlot toDomain() {
        return TimetableSlot.builder()
                .id(id)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .subjectId(subjectId != null ? subjectId.getId() : "")
                .subjectCode(subjectId != null ? subjectId.getCode() : "")
                .subjectTitle(subjectId != null ? subjectId.getTitle() : "")
                .teacherUid(teacherId != null ? teacherId.getId() : "")
                .teacherName(teacherId != null ? teacherId.getFullName() : "")
                .dayOfWeek(DayOfWeek.fromStringOrNull(dayOfWeek))
                .startTimeMinutes(startTimeMinutes)
                .endTimeMinutes(endTimeMinutes)
                .room(room)
                .createdBy(createdBy)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
