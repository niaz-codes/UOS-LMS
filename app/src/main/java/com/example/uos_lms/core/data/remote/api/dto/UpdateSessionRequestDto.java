package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

/** Both fields optional (Gson omits nulls) - backend only touches what's present. */
@Data
@Builder
public class UpdateSessionRequestDto {
    private final String label;
    private final Boolean isActive;
}
