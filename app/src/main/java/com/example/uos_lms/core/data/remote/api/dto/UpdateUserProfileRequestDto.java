package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserProfileRequestDto {
    private final String fullName;
    private final String fatherName;
    private final String phone;
    private final String cnic;
}
