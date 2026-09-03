package com.example.uos_lms.feature.messaging.presentation.compose;

import com.example.uos_lms.core.domain.model.User;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class NewConversationUiState {
    @Builder.Default
    private final List<User> contacts = Collections.emptyList();
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean creating = false;
    private final String errorMessage;

    public static NewConversationUiState initial() {
        return NewConversationUiState.builder().build();
    }
}
