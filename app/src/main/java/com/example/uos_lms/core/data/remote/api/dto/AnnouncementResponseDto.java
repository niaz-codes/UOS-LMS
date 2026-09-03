package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Announcement;
import com.example.uos_lms.core.domain.model.AnnouncementScope;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnnouncementResponseDto {
    @SerializedName("_id")
    private String id;
    private String title;
    private String body;
    private PersonRefDto authorId;
    private String scope;
    private DepartmentResponseDto departmentId;
    private SubjectRefDto subjectId;
    private String createdAt;

    public Announcement toDomain() {
        return Announcement.builder()
                .id(id)
                .title(title)
                .body(body)
                .authorUid(authorId != null ? authorId.getId() : null)
                .authorName(authorId != null ? authorId.getFullName() : "")
                .scope(AnnouncementScope.fromStringOrNull(scope))
                .departmentId(departmentId != null ? departmentId.getId() : null)
                .departmentName(departmentId != null ? departmentId.getName() : null)
                .subjectId(subjectId != null ? subjectId.getId() : null)
                .subjectName(subjectId != null ? subjectId.getTitle() : null)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
