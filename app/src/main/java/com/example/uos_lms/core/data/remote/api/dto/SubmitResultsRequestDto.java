package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitResultsRequestDto {
    private final String subjectId;
    private final List<StudentMarksDto> results;
}
