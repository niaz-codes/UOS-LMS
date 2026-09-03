package com.example.uos_lms.core.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Conversation {
    @Builder.Default
    private final String id = "";
    @Builder.Default
    private final String participantAUid = "";
    @Builder.Default
    private final String participantAName = "";
    @Builder.Default
    private final String participantARole = "";
    @Builder.Default
    private final String participantBUid = "";
    @Builder.Default
    private final String participantBName = "";
    @Builder.Default
    private final String participantBRole = "";
    private final String lastMessageText;
    private final String lastMessageSenderUid;
    private final Long lastMessageAt;
    @Builder.Default
    private final int unreadCountA = 0;
    @Builder.Default
    private final int unreadCountB = 0;
    @Builder.Default
    private final boolean archivedByA = false;
    @Builder.Default
    private final boolean archivedByB = false;
    @Builder.Default
    private final boolean deletedByA = false;
    @Builder.Default
    private final boolean deletedByB = false;
    @Builder.Default
    private final long createdAt = 0L;

    public String otherParticipantName(String myUid) {
        return participantAUid.equals(myUid) ? participantBName : participantAName;
    }

    public String otherParticipantUid(String myUid) {
        return participantAUid.equals(myUid) ? participantBUid : participantAUid;
    }

    public String otherParticipantRole(String myUid) {
        return participantAUid.equals(myUid) ? participantBRole : participantARole;
    }

    public int unreadCountFor(String myUid) {
        return participantAUid.equals(myUid) ? unreadCountA : unreadCountB;
    }
}
