package com.example.uos_lms.feature.calendar.presentation;

import com.example.uos_lms.core.domain.model.CalendarEvent;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AcademicCalendarUiState {
    @Builder.Default
    private final List<CalendarEvent> events = Collections.emptyList();
    @Builder.Default
    private final boolean admin = false;
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static AcademicCalendarUiState initial() {
        return AcademicCalendarUiState.builder().build();
    }
}
