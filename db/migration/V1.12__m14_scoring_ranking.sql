-- V1.12  M14 Scoring & Ranking
-- competition/submission/round refs are logical cross-module — no FK.

CREATE TABLE scoring_configurations (
    id              UUID          NOT NULL,
    competition_id  UUID          NOT NULL,          -- REF → competitions (config series scope)
    version_no      INT           NOT NULL,
    weight_audience NUMERIC(5,2)  NOT NULL,
    weight_judge    NUMERIC(5,2)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_scoring_configurations PRIMARY KEY (id),
    CONSTRAINT ck_sc_weights CHECK (weight_audience + weight_judge = 100),
    CONSTRAINT uq_sc_comp_version UNIQUE (competition_id, version_no)
);

CREATE TABLE tie_break_configurations (
    id             UUID        NOT NULL,
    competition_id UUID        NOT NULL,
    version_no     INT         NOT NULL,
    criteria       JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tie_break_configurations PRIMARY KEY (id),
    CONSTRAINT uq_tbc_comp_version UNIQUE (competition_id, version_no)
);

CREATE TABLE qualification_configurations (
    id             UUID        NOT NULL,
    competition_id UUID        NOT NULL,
    version_no     INT         NOT NULL,
    rule_payload   JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_qualification_configurations PRIMARY KEY (id),
    CONSTRAINT uq_qc_comp_version UNIQUE (competition_id, version_no)
);

CREATE TABLE judge_score_aggregations (               -- derived/rebuildable
    id                UUID          NOT NULL,
    submission_id     UUID          NOT NULL,         -- REF → submissions
    round_id          UUID          NOT NULL,         -- REF → competition_rounds
    config_version_id UUID          NOT NULL,         -- REF → scoring_configurations
    aggregate         NUMERIC(10,4),
    computed_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_judge_score_aggregations PRIMARY KEY (id)
);
CREATE INDEX idx_jsa_sub_round ON judge_score_aggregations (submission_id, round_id);

CREATE TABLE audience_score_aggregations (            -- derived from votes
    id                UUID          NOT NULL,
    submission_id     UUID          NOT NULL,
    round_id          UUID          NOT NULL,
    config_version_id UUID          NOT NULL,
    aggregate         NUMERIC(10,4),
    computed_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_audience_score_aggregations PRIMARY KEY (id)
);
CREATE INDEX idx_asa_sub_round ON audience_score_aggregations (submission_id, round_id);

CREATE TABLE final_scores (                            -- sealed, reproducible
    id                UUID          NOT NULL,
    submission_id     UUID          NOT NULL,          -- REF
    competition_id    UUID          NOT NULL,          -- REF
    category_id       UUID          NOT NULL,          -- REF
    round_id          UUID          NOT NULL,          -- REF
    scoring_config_id UUID          NOT NULL,          -- REF → scoring_configurations
    score_version     INT           NOT NULL,
    final_score       NUMERIC(10,4) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    sealed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_final_scores PRIMARY KEY (id),
    CONSTRAINT uq_fs_version UNIQUE (submission_id, round_id, scoring_config_id, score_version)
);
CREATE INDEX idx_fs_context ON final_scores (competition_id, category_id, round_id);
CREATE INDEX idx_fs_submission ON final_scores (submission_id);

CREATE TABLE rankings (                                  -- derived snapshot
    id               UUID        NOT NULL,
    competition_id   UUID        NOT NULL,
    category_id      UUID        NOT NULL,
    round_id         UUID        NOT NULL,
    submission_id    UUID        NOT NULL,
    rank             INT         NOT NULL,
    tie_break_applied JSONB,
    snapshot_version INT         NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_rankings PRIMARY KEY (id),
    CONSTRAINT uq_rank_snapshot UNIQUE (competition_id, category_id, round_id, submission_id, snapshot_version)
);
CREATE INDEX idx_rank_context ON rankings (competition_id, category_id, round_id, rank);

CREATE TABLE qualification_results (                     -- consumed by M15
    id                UUID        NOT NULL,
    submission_id     UUID        NOT NULL,
    round_id          UUID        NOT NULL,
    config_version_id UUID        NOT NULL,              -- REF → qualification_configurations
    qualified         BOOLEAN     NOT NULL,
    decided_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_qualification_results PRIMARY KEY (id)
);
CREATE INDEX idx_qr_round_sub ON qualification_results (round_id, submission_id);

CREATE TABLE score_overrides (                           -- audited; never overwrites source votes/evals
    id             UUID          NOT NULL,
    final_score_id UUID          NOT NULL,
    actor_id       UUID          NOT NULL,
    before_value   NUMERIC(10,4) NOT NULL,
    after_value    NUMERIC(10,4) NOT NULL,
    reason         TEXT          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_score_overrides PRIMARY KEY (id),
    CONSTRAINT fk_so_final_score FOREIGN KEY (final_score_id) REFERENCES final_scores (id)
);  -- append-only: no updated_at
