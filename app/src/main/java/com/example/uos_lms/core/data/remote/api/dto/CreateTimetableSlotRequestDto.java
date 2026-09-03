package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTimetableSlotRequestDto {
    private final String subjectId;
    private final String dayOfWeek;
    private final int startTimeMinutes;
    private final int endTimeMinutes;
    private final String room;
}
