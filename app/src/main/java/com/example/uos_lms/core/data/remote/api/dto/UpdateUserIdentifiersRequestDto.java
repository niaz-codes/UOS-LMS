package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserIdentifiersRequestDto {
    private final String employeeId;
    private final String designation;
    private final String registrationNumber;
    private final String rollNumber;
}
