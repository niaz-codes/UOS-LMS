package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Announcement {
    private final String id;
    private final String title;
    private final String body;
    private final String authorUid;
    private final String authorName;
    private final AnnouncementScope scope;
    private final String departmentId;
    private final String departmentName;
    private final String subjectId;
    private final String subjectName;
    @Builder.Default
    private final long createdAt = 0L;
}
