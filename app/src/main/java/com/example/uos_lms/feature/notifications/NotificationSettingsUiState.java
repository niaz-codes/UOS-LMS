package com.example.uos_lms.feature.notifications;

import com.example.uos_lms.core.domain.model.NotificationSettings;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class NotificationSettingsUiState {
    @Builder.Default
    private final NotificationSettings settings = NotificationSettings.initial();
    private final boolean loading;
    private final boolean saving;
    private final String errorMessage;

    public static NotificationSettingsUiState initial() {
        return NotificationSettingsUiState.builder().loading(true).build();
    }
}
