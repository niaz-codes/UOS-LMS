package com.example.uos_lms.feature.student.presentation.examschedule;

import com.example.uos_lms.core.domain.model.ExamSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class StudentExamScheduleUiState {
    @Builder.Default
    private final List<ExamSchedule> schedules = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static StudentExamScheduleUiState initial() {
        return StudentExamScheduleUiState.builder().build();
    }

    public List<ExamSchedule> getSortedSchedules() {
        List<ExamSchedule> sorted = new ArrayList<>(schedules);
        sorted.sort((a, b) -> {
            int cmp = Long.compare(a.getExamDateMillis(), b.getExamDateMillis());
            return cmp != 0 ? cmp : Integer.compare(a.getStartTimeMinutes(), b.getStartTimeMinutes());
        });
        return sorted;
    }
}
