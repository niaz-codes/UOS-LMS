package com.example.uos_lms.feature.messaging.presentation.inbox;

import com.example.uos_lms.core.domain.model.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class MessagingInboxUiState {
    @Builder.Default
    private final List<Conversation> conversations = Collections.emptyList();
    @Builder.Default
    private final String myUid = "";
    @Builder.Default
    private final boolean loading = true;
    private final String errorMessage;

    public static MessagingInboxUiState initial() {
        return MessagingInboxUiState.builder().build();
    }

    public List<Conversation> getSortedConversations() {
        List<Conversation> sorted = new ArrayList<>(conversations);
        sorted.sort((a, b) -> {
            long aTime = a.getLastMessageAt() != null ? a.getLastMessageAt() : a.getCreatedAt();
            long bTime = b.getLastMessageAt() != null ? b.getLastMessageAt() : b.getCreatedAt();
            return Long.compare(bTime, aTime);
        });
        return sorted;
    }
}
