package com.example.uos_lms.core.data.remote.api.dto;

import com.example.uos_lms.core.data.remote.api.IsoDates;
import com.example.uos_lms.core.domain.model.Message;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.NoArgsConstructor;

/** senderId/recipientId come back as plain string ids here (messagingController never
 * populates them on send/list) - senderName/senderRole are filled in client-side from the
 * already-loaded Conversation's two participants, see MessageThreadViewModel. */
@Data
@NoArgsConstructor
public class MessageResponseDto {
    @SerializedName("_id")
    private String id;
    private String conversationId;
    private String senderId;
    private String recipientId;
    private String text;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentPublicId;
    private String attachmentResourceType;
    private Long attachmentSize;
    private String sentAt;
    private String deliveredAt;
    private String readAt;

    public Message toDomain() {
        return Message.builder()
                .id(id)
                .conversationId(conversationId)
                .senderUid(senderId)
                .recipientUid(recipientId)
                .text(text != null ? text : "")
                .attachmentUrl(attachmentUrl)
                .attachmentName(attachmentName)
                .attachmentPublicId(attachmentPublicId)
                .attachmentResourceType(attachmentResourceType)
                .attachmentSize(attachmentSize)
                .sentAt(IsoDates.toMillis(sentAt))
                .deliveredAt(deliveredAt != null ? IsoDates.toMillis(deliveredAt) : null)
                .readAt(readAt != null ? IsoDates.toMillis(readAt) : null)
                .build();
    }
}
