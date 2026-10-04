package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "final_outputs")
public class FinalOutputEntity {

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;                          // REF → media_assets (no FK)

    @Column(name = "finalized_at", nullable = false)
    private OffsetDateTime finalizedAt = OffsetDateTime.now();

    @Column(name = "output_type", length = 40)
    private String outputType;

    protected FinalOutputEntity() {}

    public FinalOutputEntity(UUID roomId, UUID mediaId, String outputType) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.mediaId = mediaId;
        this.outputType = outputType;
    }

    public UUID getId() { return id; }
}
