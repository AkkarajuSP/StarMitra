package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_idempotency")
public class MessageIdempotencyEntity {

    @Id
    @Column(name = "client_message_id", length = 128)
    private String clientMessageId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected MessageIdempotencyEntity() {}

    public MessageIdempotencyEntity(String clientMessageId, UUID messageId, UUID userId) {
        this.clientMessageId = clientMessageId;
        this.messageId = messageId;
        this.userId = userId;
    }

    public UUID getMessageId() { return messageId; }
    public UUID getUserId() { return userId; }
}
