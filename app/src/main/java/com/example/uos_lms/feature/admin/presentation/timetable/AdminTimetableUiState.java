package com.example.uos_lms.feature.admin.presentation.timetable;

import com.example.uos_lms.core.domain.model.DayOfWeek;
import com.example.uos_lms.core.domain.model.Department;
import com.example.uos_lms.core.domain.model.Semester;
import com.example.uos_lms.core.domain.model.Subject;
import com.example.uos_lms.core.domain.model.TimetableSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AdminTimetableUiState {
    @Builder.Default
    private final List<Department> departments = Collections.emptyList();
    @Builder.Default
    private final List<Semester> semesters = Collections.emptyList();
    @Builder.Default
    private final List<Subject> subjects = Collections.emptyList();
    @Builder.Default
    private final List<TimetableSlot> allSlots = Collections.emptyList();
    private final Department selectedDepartment;
    private final Semester selectedSemester;
    @Builder.Default
    private final DayOfWeek selectedDay = DayOfWeek.MONDAY;
    @Builder.Default
    private final boolean saving = false;
    private final String errorMessage;
    private final String actionMessage;

    public static AdminTimetableUiState initial() {
        return AdminTimetableUiState.builder().build();
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
