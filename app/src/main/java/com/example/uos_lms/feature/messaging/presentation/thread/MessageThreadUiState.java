package com.example.uos_lms.feature.messaging.presentation.thread;

import com.example.uos_lms.core.domain.model.Conversation;
import com.example.uos_lms.core.domain.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class MessageThreadUiState {
    @Builder.Default
    private final List<Message> messages = Collections.emptyList();
    private final Conversation conversation;
    @Builder.Default
    private final String myUid = "";
    @Builder.Default
    private final boolean loading = true;
    @Builder.Default
    private final boolean sending = false;
    private final String errorMessage;

    public static MessageThreadUiState initial() {
        return MessageThreadUiState.builder().build();
    }

    public List<Message> getSortedMessages() {
        List<Message> sorted = new ArrayList<>(messages);
        sorted.sort((a, b) -> Long.compare(a.getSentAt(), b.getSentAt()));
        return sorted;
    }
}
