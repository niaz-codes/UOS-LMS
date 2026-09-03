package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSubjectRequestDto {
    private final String departmentId;
    private final String semesterId;
    private final String code;
    private final String title;
    private final int creditHours;
}
