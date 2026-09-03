package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveAttendanceRequestDto {
    private final String subjectId;
    private final String departmentId;
    private final String semesterId;
    private final String dateKey;
    private final List<StudentStatusDto> records;
}
