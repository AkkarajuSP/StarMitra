-- V1.19  M05 Discovery / Search / Feed — derived read-model tables (rebuildable)

CREATE TABLE discovery_projections (
    id           UUID        NOT NULL,
    entity_type  VARCHAR(40) NOT NULL,
    entity_id    UUID        NOT NULL,
    payload      JSONB,
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_discovery_projections PRIMARY KEY (id),
    CONSTRAINT uq_dp_entity UNIQUE (entity_type, entity_id)
);
CREATE INDEX idx_dp_entity ON discovery_projections (entity_type, entity_id);

CREATE TABLE feed_projections (
    id           UUID          NOT NULL,
    user_id      UUID          NOT NULL,
    item_type    VARCHAR(40)   NOT NULL,
    item_id      UUID          NOT NULL,
    rank_score   NUMERIC(12,4),
    refreshed_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_feed_projections PRIMARY KEY (id)
);
CREATE INDEX idx_fp_user ON feed_projections (user_id, rank_score DESC);
