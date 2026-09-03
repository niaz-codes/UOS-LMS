package com.example.uos_lms.core.data.remote.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordRequestDto {
    private final String email;
    private final String code;
    private final String newPassword;
}
