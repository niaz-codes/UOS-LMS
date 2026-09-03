package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Session {
    private final String id;
    private final String departmentId;
    private final String label;
    @Builder.Default
    private final boolean isActive = true;
    @Builder.Default
    private final long createdAt = 0L;
}
