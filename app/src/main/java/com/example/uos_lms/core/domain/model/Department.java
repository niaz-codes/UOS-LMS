package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Department {
    private final String id;
    private final String name;
    private final String code;
    @Builder.Default
    private final String description = "";
    @Builder.Default
    private final long createdAt = 0L;
}
