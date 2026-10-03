-- V1.7  M09 Competitions
-- skill/config/user/project refs are logical cross-module — no FK.

CREATE TABLE competitions (
    id                   UUID         NOT NULL,
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    config_status        VARCHAR(20)  NOT NULL,
    participation_status VARCHAR(20)  NOT NULL,
    round_state          VARCHAR(20)  NOT NULL,
    created_by           UUID         NOT NULL,          -- REF → users
    version              INT          NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_competitions PRIMARY KEY (id)
);
CREATE INDEX idx_comp_status ON competitions (config_status, participation_status);

CREATE TABLE competition_categories (
    id             UUID         NOT NULL,
    competition_id UUID         NOT NULL,
    name           VARCHAR(120) NOT NULL,
    description    TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_competition_categories PRIMARY KEY (id),
    CONSTRAINT uq_cc_comp_name UNIQUE (competition_id, name),
    CONSTRAINT fk_cc_comp FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE
);

CREATE TABLE competition_category_skills (
    category_id UUID NOT NULL,
    skill_id    UUID NOT NULL,                  -- REF → talent_skills
    CONSTRAINT pk_competition_category_skills PRIMARY KEY (category_id, skill_id),
    CONSTRAINT fk_ccs_category FOREIGN KEY (category_id) REFERENCES competition_categories (id) ON DELETE CASCADE
);
CREATE INDEX idx_ccs_skill ON competition_category_skills (skill_id);

CREATE TABLE competition_rounds (
    id                     UUID         NOT NULL,
    competition_id         UUID         NOT NULL,
    sequence               INT          NOT NULL,
    name                   VARCHAR(120),
    start_at               TIMESTAMPTZ,
    end_at                 TIMESTAMPTZ,
    vote_config_id         UUID,                        -- REF → vote_configs
    rubric_version_id      UUID,                        -- REF → evaluation_template_versions
    progression_config_id  UUID,                        -- REF → progression_configurations
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_competition_rounds PRIMARY KEY (id),
    CONSTRAINT uq_cr_comp_seq UNIQUE (competition_id, sequence),
    CONSTRAINT fk_cr_comp FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE
);

CREATE TABLE eligibility_rules (
    id             UUID        NOT NULL,
    competition_id UUID        NOT NULL,
    rule_type      VARCHAR(40) NOT NULL,
    rule_params    JSONB,
    version        INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_eligibility_rules PRIMARY KEY (id),
    CONSTRAINT fk_er_comp FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE
);
CREATE INDEX idx_er_comp ON eligibility_rules (competition_id);

CREATE TABLE submission_configs (
    id             UUID NOT NULL,
    competition_id UUID NOT NULL,
    category_id    UUID,
    config         JSONB NOT NULL,
    version        INT   NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_submission_configs PRIMARY KEY (id),
    CONSTRAINT fk_sc_comp FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_category FOREIGN KEY (category_id) REFERENCES competition_categories (id)
);

CREATE TABLE competition_participants (           -- DB-03: typed single table
    id               UUID        NOT NULL,
    competition_id   UUID        NOT NULL,
    category_id      UUID,
    participant_type VARCHAR(10) NOT NULL,
    user_id          UUID,                          -- REF → users
    project_id       UUID,                          -- REF → creative_rooms
    registered_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_competition_participants PRIMARY KEY (id),
    CONSTRAINT ck_cp_type CHECK (participant_type IN ('USER','PROJECT')),
    CONSTRAINT ck_cp_xor CHECK (
        (participant_type = 'USER'    AND user_id IS NOT NULL AND project_id IS NULL) OR
        (participant_type = 'PROJECT' AND project_id IS NOT NULL AND user_id IS NULL)
    ),
    CONSTRAINT uq_cp_participant UNIQUE NULLS NOT DISTINCT (competition_id, category_id, user_id, project_id),
    CONSTRAINT fk_cp_comp FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_category FOREIGN KEY (category_id) REFERENCES competition_categories (id)
);
CREATE INDEX idx_cp_user ON competition_participants (user_id);
CREATE INDEX idx_cp_project ON competition_participants (project_id);
