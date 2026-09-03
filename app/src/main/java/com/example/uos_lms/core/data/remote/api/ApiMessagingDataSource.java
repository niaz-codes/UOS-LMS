package com.example.uos_lms.core.data.remote.api;

import androidx.annotation.Nullable;

import com.example.uos_lms.core.data.remote.api.dto.ConversationResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.FindOrCreateConversationRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.MessageResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.MyMessageResponseDto;
import com.example.uos_lms.core.data.remote.api.dto.SendMessageRequestDto;
import com.example.uos_lms.core.domain.model.Conversation;
import com.example.uos_lms.core.domain.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** REST-backed replacement for FirestoreMessagingDataSource. One-shot only - the thread screen
 * polls listMessages() with a `since` cursor instead of a real-time listener (locked-in "no
 * real-time layer" decision), see MessageThreadViewModel. */
@Singleton
public class ApiMessagingDataSource {

    private final MessagingApi messagingApi;

    @Inject
    public ApiMessagingDataSource(MessagingApi messagingApi) {
        this.messagingApi = messagingApi;
    }

    public Task<Conversation> findOrCreateConversation(String otherUserId) {
        FindOrCreateConversationRequestDto request = FindOrCreateConversationRequestDto.builder().otherUserId(otherUserId).build();
        return RetrofitTasks.call(messagingApi.findOrCreate(request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getConversation().toDomain()));
    }

    public Task<List<Conversation>> listConversations() {
        return RetrofitTasks.call(messagingApi.listConversations()).onSuccessTask(envelope -> {
            List<Conversation> conversations = new ArrayList<>();
            if (envelope.getConversations() != null) {
                for (ConversationResponseDto dto : envelope.getConversations()) conversations.add(dto.toDomain());
            }
            return Tasks.forResult(conversations);
        });
    }

    public Task<Message> sendMessage(String conversationId, String text, @Nullable String mediaId) {
        SendMessageRequestDto request = SendMessageRequestDto.builder().text(text).mediaId(mediaId).build();
        return RetrofitTasks.call(messagingApi.sendMessage(conversationId, request)).onSuccessTask(envelope -> Tasks.forResult(envelope.getMessage().toDomain()));
    }

    public Task<List<Message>> listMessages(String conversationId, @Nullable Long sinceMillis) {
        String sinceIso = sinceMillis != null ? IsoDates.toIso(sinceMillis) : null;
        return RetrofitTasks.call(messagingApi.listMessages(conversationId, sinceIso)).onSuccessTask(envelope -> {
            List<Message> messages = new ArrayList<>();
            if (envelope.getMessages() != null) {
                for (MessageResponseDto dto : envelope.getMessages()) messages.add(dto.toDomain());
            }
            return Tasks.forResult(messages);
        });
    }

    public Task<Conversation> markRead(String conversationId) {
        return RetrofitTasks.call(messagingApi.markRead(conversationId)).onSuccessTask(envelope -> Tasks.forResult(envelope.getConversation().toDomain()));
    }

    /** Consumed by AppNotificationCenter - every message addressed to the caller, across every
     * conversation, newest first. */
    public Task<List<Message>> myMessages(@Nullable Long sinceMillis) {
        String sinceIso = sinceMillis != null ? IsoDates.toIso(sinceMillis) : null;
        return RetrofitTasks.call(messagingApi.listMyMessages(sinceIso)).onSuccessTask(envelope -> {
            List<Message> messages = new ArrayList<>();
            if (envelope.getMessages() != null) {
                for (MyMessageResponseDto dto : envelope.getMessages()) messages.add(dto.toDomain());
            }
            return Tasks.forResult(messages);
        });
    }
}
