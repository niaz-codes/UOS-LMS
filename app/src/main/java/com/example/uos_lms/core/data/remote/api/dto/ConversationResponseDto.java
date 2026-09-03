package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Conversation;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConversationResponseDto {
    @SerializedName("_id")
    private String id;
    private PersonRefDto participantAId;
    private PersonRefDto participantBId;
    private String lastMessageText;
    private String lastMessageSenderId;
    private String lastMessageAt;
    private int unreadCountA;
    private int unreadCountB;
    private String createdAt;

    public Conversation toDomain() {
        return Conversation.builder()
                .id(id)
                .participantAUid(participantAId != null ? participantAId.getId() : "")
                .participantAName(participantAId != null ? participantAId.getFullName() : "")
                .participantARole(participantAId != null ? participantAId.getRole() : "")
                .participantBUid(participantBId != null ? participantBId.getId() : "")
                .participantBName(participantBId != null ? participantBId.getFullName() : "")
                .participantBRole(participantBId != null ? participantBId.getRole() : "")
                .lastMessageText(lastMessageText)
                .lastMessageSenderUid(lastMessageSenderId)
                .lastMessageAt(lastMessageAt != null ? IsoDates.toMillis(lastMessageAt) : null)
                .unreadCountA(unreadCountA)
                .unreadCountB(unreadCountB)
                .createdAt(IsoDates.toMillis(createdAt))
                .build();
    }
}
