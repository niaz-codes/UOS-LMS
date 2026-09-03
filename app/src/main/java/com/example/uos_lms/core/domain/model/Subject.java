package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Subject {
    private final String id;
    private final String departmentId;
    private final String semesterId;
    private final String code;
    private final String title;
    private final int creditHours;
    private final String teacherUid;
    private final String teacherName;
    @Builder.Default
    private final long createdAt = 0L;
}
