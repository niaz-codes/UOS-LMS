package com.example.uos_lms.feature.hod.presentation.timetable;

import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.TimetableSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class HodTimetableUiState {
    @Builder.Default
    private final List<TimetableSlot> allSlots = Collections.emptyList();
    @Builder.Default
    private final DayOfWeek selectedDay = DayOfWeek.MONDAY;
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static HodTimetableUiState initial() {
        return HodTimetableUiState.builder().build();
    }

    public List<TimetableSlot> getSlotsForSelectedDay() {
        List<TimetableSlot> filtered = new ArrayList<>();
        for (TimetableSlot slot : allSlots) {
            if (slot.getDayOfWeek() == selectedDay) filtered.add(slot);
        }
        filtered.sort((a, b) -> Integer.compare(a.getStartTimeMinutes(), b.getStartTimeMinutes()));
        return filtered;
    }
}
