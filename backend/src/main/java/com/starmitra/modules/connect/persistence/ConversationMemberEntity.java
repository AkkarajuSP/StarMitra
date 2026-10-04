package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversation_members")
@IdClass(ConversationMemberEntity.Pk.class)
public class ConversationMemberEntity {

    public static class Pk implements Serializable {
        private UUID conversationId;
        private UUID userId;
        public Pk() {}
        public Pk(UUID c, UUID u) { this.conversationId = c; this.userId = u; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && conversationId.equals(p.conversationId) && userId.equals(p.userId);
        }
        @Override public int hashCode() { return Objects.hash(conversationId, userId); }
    }

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 20)
    private String role;                          // e.g. OWNER/MEMBER — conversation-level only

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    protected ConversationMemberEntity() {}

    public ConversationMemberEntity(UUID conversationId, UUID userId, String role) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
    }

    public boolean isActive() { return leftAt == null; }
    public void leave() { this.leftAt = OffsetDateTime.now(); }

    public UUID getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
}
