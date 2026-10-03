-- V1.8  M10 Submissions
-- competition/category/round/member/role/media are logical cross-module refs — no FK.

CREATE TABLE submissions (
    id             UUID        NOT NULL,
    participant_id UUID        NOT NULL,
    competition_id UUID        NOT NULL,            -- REF → competitions
    category_id    UUID        NOT NULL,            -- REF → competition_categories
    round_id       UUID        NOT NULL,            -- REF → competition_rounds
    state          VARCHAR(20) NOT NULL,
    submitted_at   TIMESTAMPTZ,
    finalized_at   TIMESTAMPTZ,
    version        INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_submissions PRIMARY KEY (id),
    CONSTRAINT fk_sub_participant FOREIGN KEY (participant_id) REFERENCES competition_participants (id)
);
CREATE INDEX idx_sub_comp_round ON submissions (competition_id, round_id);
CREATE INDEX idx_sub_participant ON submissions (participant_id);
CREATE INDEX idx_sub_state ON submissions (state);
CREATE INDEX idx_sub_submitted ON submissions (submitted_at);

CREATE TABLE submission_media (
    submission_id UUID NOT NULL,
    media_id      UUID NOT NULL,                    -- REF → media_assets
    sort_order    INT,
    CONSTRAINT pk_submission_media PRIMARY KEY (submission_id, media_id),
    CONSTRAINT fk_sm_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);
CREATE INDEX idx_sm_media ON submission_media (media_id);

CREATE TABLE submission_contributors (              -- DB-02: live ref + finalize snapshot
    id                      UUID         NOT NULL,
    submission_id           UUID         NOT NULL,
    member_ref              UUID         NOT NULL,  -- REF → project_members
    role_ref                UUID,                   -- REF → project_contribution_roles
    snapshot_member_display VARCHAR(200),
    snapshot_role_name      VARCHAR(80),
    captured_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_submission_contributors PRIMARY KEY (id),
    CONSTRAINT fk_sco_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);
CREATE INDEX idx_sco_submission ON submission_contributors (submission_id);
CREATE INDEX idx_sco_member ON submission_contributors (member_ref);

CREATE TABLE submission_history (
    id            UUID        NOT NULL,
    submission_id UUID        NOT NULL,
    from_state    VARCHAR(20),
    to_state      VARCHAR(20) NOT NULL,
    actor_id      UUID,
    reason        TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_submission_history PRIMARY KEY (id),
    CONSTRAINT fk_sh_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);  -- append-only: no updated_at
CREATE INDEX idx_sh_submission ON submission_history (submission_id);
