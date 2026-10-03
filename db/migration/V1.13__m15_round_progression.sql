-- V1.13  M15 Round Progression
-- competition/submission/round refs are logical cross-module — no FK.

CREATE TABLE progression_configurations (            -- M15-owned (CM-06); frozen once begun
    id             UUID        NOT NULL,
    competition_id UUID        NOT NULL,
    version_no     INT         NOT NULL,
    rule_payload   JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL,
    frozen_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_progression_configurations PRIMARY KEY (id),
    CONSTRAINT uq_pc_comp_version UNIQUE (competition_id, version_no)
);

CREATE TABLE progression_records (
    id                UUID        NOT NULL,
    submission_id     UUID        NOT NULL,           -- REF → submissions
    round_id          UUID        NOT NULL,           -- REF → competition_rounds (decision round)
    source_round_id   UUID,                           -- REF
    target_round_id   UUID,                           -- REF
    outcome           VARCHAR(20) NOT NULL,
    config_version_id UUID        NOT NULL,           -- REF → progression_configurations
    finalized_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_progression_records PRIMARY KEY (id),
    CONSTRAINT ck_pr_outcome CHECK (outcome IN ('ADVANCED','ELIMINATED','PENDING')),
    CONSTRAINT uq_pr_entry UNIQUE (submission_id, round_id, config_version_id)
);
CREATE INDEX idx_pr_round ON progression_records (round_id);

CREATE TABLE progression_overrides (                  -- separate from score_overrides
    id                    UUID        NOT NULL,
    progression_record_id UUID        NOT NULL,
    actor_id              UUID        NOT NULL,
    before_outcome        VARCHAR(20) NOT NULL,
    after_outcome         VARCHAR(20) NOT NULL,
    reason                TEXT        NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_progression_overrides PRIMARY KEY (id),
    CONSTRAINT fk_po_record FOREIGN KEY (progression_record_id) REFERENCES progression_records (id)
);  -- append-only
