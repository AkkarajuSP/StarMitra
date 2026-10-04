package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private long sequence;                        // server-assigned, per-conversation (uq)

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected MessageEntity() {}

    public MessageEntity(UUID conversationId, UUID senderId, long sequence, String body) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.sequence = sequence;
        this.body = body;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSenderId() { return senderId; }
    public long getSequence() { return sequence; }
    public String getBody() { return body; }
    public OffsetDateTime getSentAt() { return sentAt; }
}
