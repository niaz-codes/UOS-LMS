package com.example.uos_lms.feature.profile.presentation.settings;

import com.example.uos_lms.core.domain.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SettingsUiState {
    private final User user;
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean savingProfile = false;
    @Builder.Default
    private final boolean changingPassword = false;
    private final String errorMessage;
    private final String actionMessage;

    public static SettingsUiState initial() {
        return SettingsUiState.builder().build();
    }
}
