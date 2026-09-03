package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Subject;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Subject document shape, as returned by the university.js list/create/update/assign
 * endpoints - `teacherId` is populated server-side (see subjectController.js's
 * POPULATE_TEACHER) into a {@link PersonRefDto}, the same populated-ref shape
 * {@link SubjectRefDto} uses from the other direction inside ExamResult/RepeatExam. */
@Data
@NoArgsConstructor
public class SubjectResponseDto {
    @SerializedName("_id")
    private String id;
    private String departmentId;
    private String semesterId;
    private String code;
    private String title;
    private int creditHours;
    private PersonRefDto teacherId;
    private String createdAt;

    public Subject toDomain() {
        return Subject.builder()
                .id(id)
                .departmentId(departmentId)
                .semesterId(semesterId)
                .code(code)
                .title(title)
                .creditHours(creditHours)
                .teacherUid(teacherId != null ? teacherId.getId() : null)
                .teacherName(teacherId != null ? teacherId.getFullName() : null)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
