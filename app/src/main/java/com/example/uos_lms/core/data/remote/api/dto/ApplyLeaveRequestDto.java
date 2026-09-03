package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyLeaveRequestDto {
    private final long fromDate;
    private final long toDate;
    private final String reason;
    private final String mediaId;
}
