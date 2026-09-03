package com.example.uos_lms.core.data.remote.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindOrCreateConversationRequestDto {
    private final String otherUserId;
}
