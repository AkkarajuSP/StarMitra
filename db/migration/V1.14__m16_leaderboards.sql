-- V1.14  M16 Leaderboards

CREATE TABLE leaderboard_projections (               -- derived/rebuildable read model
    id              UUID        NOT NULL,
    competition_id  UUID        NOT NULL,
    category_id     UUID        NOT NULL,
    round_id        UUID        NOT NULL,
    entry_ref       UUID        NOT NULL,             -- REF → submissions/participants
    display_payload JSONB,
    rank            INT,
    refreshed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_leaderboard_projections PRIMARY KEY (id)
);
CREATE INDEX idx_lp_context ON leaderboard_projections (competition_id, category_id, round_id, rank);

CREATE TABLE leaderboard_publications (               -- authoritative visibility state
    id             UUID        NOT NULL,
    competition_id UUID        NOT NULL,
    category_id    UUID        NOT NULL,
    round_id       UUID        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    published_at   TIMESTAMPTZ,
    published_by   UUID,                                -- REF → users
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_leaderboard_publications PRIMARY KEY (id),
    CONSTRAINT ck_lpub_status CHECK (status IN ('HIDDEN','PUBLISHED','ARCHIVED'))
);
CREATE INDEX idx_lpub_context ON leaderboard_publications (competition_id, category_id, round_id);

CREATE TABLE leaderboard_snapshots (                   -- sealed version-bound reference
    id                        UUID        NOT NULL,
    leaderboard_publication_id UUID       NOT NULL,
    result_version_refs        JSONB      NOT NULL,
    captured_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_leaderboard_snapshots PRIMARY KEY (id),
    CONSTRAINT fk_ls_publication FOREIGN KEY (leaderboard_publication_id) REFERENCES leaderboard_publications (id)
);
