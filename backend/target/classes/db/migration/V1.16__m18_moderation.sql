-- V1.16  M18 Moderation  (no moderation_appeals — DB-07 deferred)
-- target/ref/moderator/policy refs are logical cross-module/polymorphic — no FK.

CREATE TABLE moderation_reports (
    id          UUID         NOT NULL,
    reporter_id UUID         NOT NULL,
    target_type VARCHAR(40)  NOT NULL,              -- POLY: typed ref, app-validated
    target_id   UUID         NOT NULL,
    reason_code VARCHAR(40)  NOT NULL,
    detail      TEXT,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_reports PRIMARY KEY (id),
    CONSTRAINT fk_mrep_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);
CREATE INDEX idx_mrep_target ON moderation_reports (target_type, target_id);
CREATE INDEX idx_mrep_status ON moderation_reports (status, created_at);

CREATE TABLE moderation_cases (
    id                     UUID        NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    assigned_moderator_id  UUID,                            -- REF → users
    opened_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_cases PRIMARY KEY (id)
);
CREATE INDEX idx_mcase_status ON moderation_cases (status);

CREATE TABLE moderation_decisions (                          -- never overwritten
    id                UUID        NOT NULL,
    case_id           UUID        NOT NULL,
    moderator_id      UUID        NOT NULL,                 -- REF → users
    decision_type     VARCHAR(40) NOT NULL,
    reason            TEXT        NOT NULL,
    policy_version_id UUID,                                 -- REF → moderation_policy_references
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_decisions PRIMARY KEY (id),
    CONSTRAINT fk_mdec_case FOREIGN KEY (case_id) REFERENCES moderation_cases (id)
);  -- append-only
CREATE INDEX idx_mdec_case ON moderation_decisions (case_id);

CREATE TABLE moderation_actions (
    id           UUID        NOT NULL,
    decision_id  UUID        NOT NULL,
    action_type  VARCHAR(40) NOT NULL,
    target_type  VARCHAR(40) NOT NULL,
    target_id    UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    executed_at  TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_actions PRIMARY KEY (id),
    CONSTRAINT fk_mact_decision FOREIGN KEY (decision_id) REFERENCES moderation_decisions (id)
);
CREATE INDEX idx_mact_target ON moderation_actions (target_type, target_id);

CREATE TABLE moderation_evidence_references (
    id            UUID        NOT NULL,
    case_id       UUID        NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    ref_type      VARCHAR(40) NOT NULL,              -- POLY typed ref
    ref_id        UUID        NOT NULL,
    captured_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_evidence_references PRIMARY KEY (id),
    CONSTRAINT fk_mer_case FOREIGN KEY (case_id) REFERENCES moderation_cases (id) ON DELETE CASCADE
);
CREATE INDEX idx_mer_case ON moderation_evidence_references (case_id);

CREATE TABLE moderation_restrictions (                        -- admin-enforced ≠ user_blocks
    id               UUID        NOT NULL,
    target_type      VARCHAR(40) NOT NULL,
    target_id        UUID        NOT NULL,
    restriction_type VARCHAR(40) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    starts_at        TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ,
    created_by       UUID        NOT NULL,                  -- REF → users
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_restrictions PRIMARY KEY (id)
);
CREATE INDEX idx_mres_target ON moderation_restrictions (target_type, target_id, status);

CREATE TABLE moderation_policy_references (
    id         UUID        NOT NULL,
    code       VARCHAR(60) NOT NULL,
    version_no INT         NOT NULL,
    payload    JSONB,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_moderation_policy_references PRIMARY KEY (id),
    CONSTRAINT uq_mpr_version UNIQUE (code, version_no)
);
