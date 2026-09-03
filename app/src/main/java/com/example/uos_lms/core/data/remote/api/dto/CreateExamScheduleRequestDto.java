package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateExamScheduleRequestDto {
    private final String subjectId;
    private final String examType;
    private final long examDate;
    private final int startTimeMinutes;
    private final int endTimeMinutes;
    private final String room;
    private final String invigilatorId;
}
