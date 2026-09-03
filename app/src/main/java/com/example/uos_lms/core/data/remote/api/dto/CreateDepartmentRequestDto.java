package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateDepartmentRequestDto {
    private final String name;
    private final String code;
    private final String description;
}
