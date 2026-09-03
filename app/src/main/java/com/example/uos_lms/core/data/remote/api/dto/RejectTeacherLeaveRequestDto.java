package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RejectTeacherLeaveRequestDto {
    private final String reason;
}
