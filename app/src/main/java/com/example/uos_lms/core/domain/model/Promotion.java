package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * Immutable audit record of one student's semester promotion, doc id
 * {@code studentUid + "_" + fromSemesterId} (same deterministic-id pattern as
 * ExamResult) - a second promotion attempt from the same semester overwrites
 * the same doc rather than creating a duplicate, which is what makes
 * "already promoted" detection a plain existence check.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Promotion {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String studentUid = "";
    @Builder.Default
    private final String studentName = "";
    @Builder.Default
    private final String departmentId = "";
    @Builder.Default
    private final String sessionId = "";
    @Builder.Default
    private final String fromSemesterId = "";
    @Builder.Default
    private final int fromSemesterNumber = 0;
    @Builder.Default
    private final String toSemesterId = "";
    @Builder.Default
    private final int toSemesterNumber = 0;
    @Builder.Default
    private final List<String> failedSubjectIds = Collections.emptyList();
    @Builder.Default
    private final String promotedByUid = "";
    @Builder.Default
    private final String promotedByName = "";
    @Builder.Default
    private final long promotedAt = 0L;
}
