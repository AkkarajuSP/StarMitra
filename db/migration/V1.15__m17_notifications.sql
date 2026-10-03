-- V1.15  M17 Notifications
-- template_id/source refs are logical cross-module/polymorphic — no FK.

CREATE TABLE notifications (
    id              UUID         NOT NULL,
    recipient_id    UUID         NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    template_id     UUID,                             -- REF → notification_templates
    body            TEXT,
    source_ref_type VARCHAR(40),
    source_ref_id   UUID,
    deep_link       VARCHAR(512),
    state           VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);
CREATE INDEX idx_notif_recipient ON notifications (recipient_id, created_at);
CREATE INDEX idx_notif_recipient_state ON notifications (recipient_id, state);

CREATE TABLE notification_preferences (
    user_id UUID        NOT NULL,
    type    VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (user_id, type, channel),
    CONSTRAINT fk_np_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE notification_templates (
    id            UUID        NOT NULL,
    code          VARCHAR(60) NOT NULL,
    version_no    INT         NOT NULL,
    channel       VARCHAR(20) NOT NULL,
    body_template TEXT        NOT NULL,
    locale        VARCHAR(10),
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_notification_templates PRIMARY KEY (id),
    CONSTRAINT uq_nt_version UNIQUE NULLS NOT DISTINCT (code, channel, version_no, locale)
);

CREATE TABLE notification_delivery_attempts (
    id              UUID        NOT NULL,
    notification_id UUID        NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    attempted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    retry_count     INT         NOT NULL DEFAULT 0,
    failure_reason  TEXT,
    CONSTRAINT pk_notification_delivery_attempts PRIMARY KEY (id),
    CONSTRAINT fk_nda_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
);
CREATE INDEX idx_nda_notification ON notification_delivery_attempts (notification_id);
CREATE INDEX idx_nda_status ON notification_delivery_attempts (status);

CREATE TABLE notification_read_states (
    notification_id UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    read_at         TIMESTAMPTZ,
    CONSTRAINT pk_notification_read_states PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_nrs_notification FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_nrs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE notification_event_references (            -- dedup key for source events
    id            UUID         NOT NULL,
    source_module VARCHAR(20)  NOT NULL,
    event_type    VARCHAR(60)  NOT NULL,
    source_id     VARCHAR(128) NOT NULL,
    event_version VARCHAR(40),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_notification_event_references PRIMARY KEY (id),
    CONSTRAINT uq_ner_dedup UNIQUE NULLS NOT DISTINCT (source_module, event_type, source_id, event_version)
);
