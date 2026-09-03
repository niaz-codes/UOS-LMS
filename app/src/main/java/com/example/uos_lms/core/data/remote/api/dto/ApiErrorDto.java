package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiErrorDto {
    private ErrorBody error;

    @Data
    @NoArgsConstructor
    public static class ErrorBody {
        private String message;
        private String accountStatus;
    }
}
