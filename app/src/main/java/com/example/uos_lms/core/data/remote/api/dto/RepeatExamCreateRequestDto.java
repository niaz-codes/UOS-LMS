package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepeatExamCreateRequestDto {
    private final String examResultId;
}
