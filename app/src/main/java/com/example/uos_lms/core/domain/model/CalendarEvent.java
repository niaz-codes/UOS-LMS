package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class CalendarEvent {
    private final String id;
    private final String title;
    private final String description;
    private final CalendarEventType type;
    private final long dateMillis;
    private final String createdBy;
    @Builder.Default
    private final long createdAt = 0L;
}
