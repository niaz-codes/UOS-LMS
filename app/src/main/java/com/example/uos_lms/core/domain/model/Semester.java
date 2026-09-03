package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Semester {
    private final String id;
    private final String departmentId;
    private final int number;
    @Builder.Default
    private final long createdAt = 0L;

    public String getDisplayName() {
        return "Semester " + number;
    }
}
