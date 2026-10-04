package com.starmitra.modules.connect.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "message_attachments")
public class MessageAttachmentEntity {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;                          // REF → media_assets (no FK)

    @Column(name = "sort_order")
    private Integer sortOrder;

    protected MessageAttachmentEntity() {}

    public MessageAttachmentEntity(UUID messageId, UUID mediaId, Integer sortOrder) {
        this.id = UUID.randomUUID();
        this.messageId = messageId;
        this.mediaId = mediaId;
        this.sortOrder = sortOrder;
    }

    public UUID getMediaId() { return mediaId; }
}
