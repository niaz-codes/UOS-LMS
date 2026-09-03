package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangePasswordRequestDto {
    private final String currentPassword;
    private final String newPassword;
}
