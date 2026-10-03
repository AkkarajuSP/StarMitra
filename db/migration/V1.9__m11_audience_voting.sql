-- V1.9  M11 Audience Voting
-- competition/category/round are logical cross-module refs — no FK.

CREATE TABLE votes (
    id             UUID         NOT NULL,
    voter_id       UUID         NOT NULL,
    submission_id  UUID         NOT NULL,
    competition_id UUID         NOT NULL,            -- REF → competitions
    category_id    UUID         NOT NULL,            -- REF → competition_categories
    round_id       UUID         NOT NULL,            -- REF → competition_rounds
    target_type    VARCHAR(10)  NOT NULL,
    cast_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    client_msg_id  VARCHAR(128),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_votes PRIMARY KEY (id),
    CONSTRAINT ck_votes_target CHECK (target_type IN ('INDIVIDUAL','PROJECT')),
    CONSTRAINT uq_votes_dedup UNIQUE (voter_id, submission_id, round_id),
    CONSTRAINT uq_votes_client UNIQUE (client_msg_id),
    CONSTRAINT fk_votes_voter FOREIGN KEY (voter_id) REFERENCES users (id),
    CONSTRAINT fk_votes_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);
CREATE INDEX idx_votes_submission ON votes (submission_id);
CREATE INDEX idx_votes_comp_round ON votes (competition_id, round_id);

CREATE TABLE vote_configs (                         -- M11 behavior config; M09 holds REF only
    id           UUID        NOT NULL,
    series_key   VARCHAR(80) NOT NULL,               -- identifies the config series being versioned
    version_no   INT         NOT NULL,
    payload      JSONB       NOT NULL,
    status       VARCHAR(20) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_vote_configs PRIMARY KEY (id),
    CONSTRAINT uq_vc_series_version UNIQUE (series_key, version_no)
);
