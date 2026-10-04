package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/** Source-event dedup key — UQ(module,event_type,source_id,event_version). */
@Entity
@Table(name = "notification_event_references")
public class NotificationEventReferenceEntity {

    @Id
    private UUID id;

    @Column(name = "source_module", nullable = false, length = 20)
    private String sourceModule;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;

    @Column(name = "event_version", length = 40)
    private String eventVersion;

    protected NotificationEventReferenceEntity() {}

    public NotificationEventReferenceEntity(String sourceModule, String eventType,
                                            String sourceId, String eventVersion) {
        this.id = UUID.randomUUID();
        this.sourceModule = sourceModule;
        this.eventType = eventType;
        this.sourceId = sourceId;
        this.eventVersion = eventVersion;
    }
}
