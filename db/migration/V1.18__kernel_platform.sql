-- V1.18  Platform Kernel  (audit ≠ telemetry ≠ analytics; M19 does not own these)

CREATE TABLE audit_log (                              -- append-only: insert-only grants at deploy
    id             UUID         NOT NULL,
    actor_id       UUID,
    actor_context  VARCHAR(80),
    module         VARCHAR(20)  NOT NULL,
    action         VARCHAR(80)  NOT NULL,
    target_type    VARCHAR(60),
    target_id      VARCHAR(80),
    before_ref     JSONB,
    after_ref      JSONB,
    reason         TEXT,
    correlation_id UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);  -- no updated_at — immutable record
CREATE INDEX idx_audit_target ON audit_log (target_type, target_id);
CREATE INDEX idx_audit_actor ON audit_log (actor_id);
CREATE INDEX idx_audit_correlation ON audit_log (correlation_id);
CREATE INDEX idx_audit_created ON audit_log (created_at);

CREATE TABLE platform_config (
    config_key   VARCHAR(120) NOT NULL,
    config_value JSONB,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by   UUID,
    CONSTRAINT pk_platform_config PRIMARY KEY (config_key)
);

CREATE TABLE analytics_projections (                  -- ADR-013 derived — never authoritative
    id              UUID        NOT NULL,
    projection_type VARCHAR(60) NOT NULL,
    dims            JSONB,
    metrics         JSONB,
    refreshed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_analytics_projections PRIMARY KEY (id)
);
CREATE INDEX idx_ap_type ON analytics_projections (projection_type);
