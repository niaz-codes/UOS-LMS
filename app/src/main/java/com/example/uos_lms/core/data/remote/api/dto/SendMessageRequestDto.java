package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMessageRequestDto {
    private final String text;
    private final String mediaId;
}
