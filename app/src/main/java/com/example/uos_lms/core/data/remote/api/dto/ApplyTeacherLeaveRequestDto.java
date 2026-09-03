package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyTeacherLeaveRequestDto {
    private final String leaveType;
    private final long fromDate;
    private final long toDate;
    private final String reason;
}
