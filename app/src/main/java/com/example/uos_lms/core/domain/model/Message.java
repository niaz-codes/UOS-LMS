package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Message {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String conversationId = "";
    @Builder.Default
    private final String senderUid = "";
    @Builder.Default
    private final String senderName = "";
    @Builder.Default
    private final String senderRole = "";
    @Builder.Default
    private final String recipientUid = "";
    @Builder.Default
    private final String text = "";
    private final String attachmentUrl;
    private final String attachmentName;
    private final String attachmentPublicId;
    private final String attachmentResourceType;
    private final Long attachmentSize;
    @Builder.Default
    private final long sentAt = 0L;
    /** Null = not yet delivered/read - the recipient's client hasn't fetched (deliveredAt) or
     * opened the thread for (readAt) this message. Drives the single/double/blue-tick UI. */
    private final Long deliveredAt;
    private final Long readAt;
}
