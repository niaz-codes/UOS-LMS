package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Message;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Shape of GET /messages/mine - unlike MessageResponseDto (used by send/list on a single
 * conversation), this endpoint populates senderId with {fullName} since AppNotificationCenter
 * has no Conversation object on hand to cross-reference a name from. */
@Data
@NoArgsConstructor
public class MyMessageResponseDto {
    @SerializedName("_id")
    private String id;
    private String conversationId;
    private PersonRefDto senderId;
    private String text;
    private String sentAt;

    public Message toDomain() {
        return Message.builder()
                .id(id)
                .conversationId(conversationId)
                .senderUid(senderId != null ? senderId.getId() : null)
                .senderName(senderId != null ? senderId.getFullName() : "")
                .text(text != null ? text : "")
                .sentAt(IsoDates.toMillis(sentAt))
                .build();
    }
}
