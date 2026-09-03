package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepeatExamMarksRequestDto {
    private final double marks;
}
