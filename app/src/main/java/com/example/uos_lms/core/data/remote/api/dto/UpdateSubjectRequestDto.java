package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateSubjectRequestDto {
    private final String code;
    private final String title;
    private final Integer creditHours;
}
