package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "message_receipts")
@IdClass(MessageReceiptEntity.Pk.class)
public class MessageReceiptEntity {

    public enum Status { DELIVERED, READ }

    public static class Pk implements Serializable {
        private UUID messageId;
        private UUID userId;
        public Pk() {}
        public Pk(UUID m, UUID u) { this.messageId = m; this.userId = u; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && messageId.equals(p.messageId) && userId.equals(p.userId);
        }
        @Override public int hashCode() { return Objects.hash(messageId, userId); }
    }

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected MessageReceiptEntity() {}

    public MessageReceiptEntity(UUID messageId, UUID userId, Status status) {
        this.messageId = messageId;
        this.userId = userId;
        this.status = status;
    }

    public void markRead() { this.status = Status.READ; this.updatedAt = OffsetDateTime.now(); }

    public UUID getMessageId() { return messageId; }
    public Status getStatus() { return status; }
}
