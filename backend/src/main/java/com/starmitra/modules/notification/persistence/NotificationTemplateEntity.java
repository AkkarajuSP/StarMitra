package com.starmitra.modules.notification.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/** Versioned server-side template — body_template with {param} placeholders. */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Column(length = 10)
    private String locale;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    protected NotificationTemplateEntity() {}

    public NotificationTemplateEntity(String code, int versionNo, String channel,
                                      String bodyTemplate, String locale) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.versionNo = versionNo;
        this.channel = channel;
        this.bodyTemplate = bodyTemplate;
        this.locale = locale;
    }

    public UUID getId() { return id; }
    public String getBodyTemplate() { return bodyTemplate; }
}
