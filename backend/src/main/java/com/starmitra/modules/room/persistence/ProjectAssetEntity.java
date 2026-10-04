package com.starmitra.modules.room.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "project_assets")
public class ProjectAssetEntity {

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;                          // REF → media_assets (no FK)

    @Column(length = 40)
    private String role;                           // e.g. SCRIPT, STORYBOARD — freeform tag

    protected ProjectAssetEntity() {}

    public ProjectAssetEntity(UUID roomId, UUID mediaId, String role) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.mediaId = mediaId;
        this.role = role;
    }

    public UUID getId() { return id; }
    public UUID getMediaId() { return mediaId; }
}
