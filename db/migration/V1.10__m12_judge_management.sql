-- V1.10  M12 Judge Management
-- competition/category/round/skill are logical cross-module refs — no FK.

CREATE TABLE judges (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_judges PRIMARY KEY (id),
    CONSTRAINT uq_judges_user UNIQUE (user_id),
    CONSTRAINT fk_judge_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE judge_expertise (
    id           UUID         NOT NULL,
    judge_id     UUID         NOT NULL,
    skill_id     UUID,                          -- REF → talent_skills
    domain_label VARCHAR(120),
    verified     BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_judge_expertise PRIMARY KEY (id),
    CONSTRAINT fk_je_judge FOREIGN KEY (judge_id) REFERENCES judges (id)
);
CREATE INDEX idx_je_judge ON judge_expertise (judge_id);

CREATE TABLE judge_assignments (                    -- DB-08: inline scope; the authz boundary
    id             UUID        NOT NULL,
    judge_id       UUID        NOT NULL,
    competition_id UUID        NOT NULL,            -- REF → competitions
    category_id    UUID,                            -- REF → competition_categories
    round_id       UUID,                            -- REF → competition_rounds
    status         VARCHAR(20) NOT NULL,
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_judge_assignments PRIMARY KEY (id),
    CONSTRAINT ck_ja_status CHECK (status IN ('PENDING','ACTIVE','REVOKED','COMPLETED')),
    CONSTRAINT uq_ja_scope UNIQUE NULLS NOT DISTINCT (judge_id, competition_id, category_id, round_id),
    CONSTRAINT fk_ja_judge FOREIGN KEY (judge_id) REFERENCES judges (id)
);
CREATE INDEX idx_ja_judge ON judge_assignments (judge_id);
CREATE INDEX idx_ja_scope ON judge_assignments (competition_id, category_id, round_id);
