package com.example.uos_lms.core.data.remote.api.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationsEnvelopeDto {
    private List<NotificationResponseDto> notifications;
    private int total;
    private int page;
    private int limit;
    private int unreadCount;
}
