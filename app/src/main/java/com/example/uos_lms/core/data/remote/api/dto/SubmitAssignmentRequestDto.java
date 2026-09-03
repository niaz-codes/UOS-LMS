package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitAssignmentRequestDto {
    private final String textAnswer;
    private final String mediaId;
}
