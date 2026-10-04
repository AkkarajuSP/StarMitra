package com.starmitra.modules.portfolio.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "portfolio_item_media")
@IdClass(PortfolioItemMediaEntity.Pk.class)
public class PortfolioItemMediaEntity {

    public static class Pk implements Serializable {
        private UUID itemId;
        private UUID mediaId;
        public Pk() {}
        public Pk(UUID itemId, UUID mediaId) { this.itemId = itemId; this.mediaId = mediaId; }
        @Override public boolean equals(Object o) {
            return o instanceof Pk p && itemId.equals(p.itemId) && mediaId.equals(p.mediaId);
        }
        @Override public int hashCode() { return Objects.hash(itemId, mediaId); }
    }

    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Id
    @Column(name = "media_id")
    private UUID mediaId;                         // REF → media_assets (no FK)

    @Column(name = "sort_order")
    private Integer sortOrder;

    protected PortfolioItemMediaEntity() {}

    public PortfolioItemMediaEntity(UUID itemId, UUID mediaId, Integer sortOrder) {
        this.itemId = itemId;
        this.mediaId = mediaId;
        this.sortOrder = sortOrder;
    }

    public UUID getItemId() { return itemId; }
    public UUID getMediaId() { return mediaId; }
}
