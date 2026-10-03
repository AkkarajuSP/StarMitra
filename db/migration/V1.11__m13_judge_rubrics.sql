-- V1.11  M13 Judge Rubrics
-- judge/assignment/submission/competition refs are logical cross-module — no FK.

CREATE TABLE evaluation_templates (
    id         UUID         NOT NULL,
    name       VARCHAR(160) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_by UUID,                              -- REF → users
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_evaluation_templates PRIMARY KEY (id)
);

CREATE TABLE evaluation_template_versions (          -- immutable once PUBLISHED (app-enforced)
    id           UUID        NOT NULL,
    template_id  UUID        NOT NULL,
    version_no   INT         NOT NULL,
    status       VARCHAR(20) NOT NULL,
    payload      JSONB       NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_evaluation_template_versions PRIMARY KEY (id),
    CONSTRAINT ck_etv_status CHECK (status IN ('DRAFT','VALIDATED','PUBLISHED','RETIRED')),
    CONSTRAINT uq_etv_version UNIQUE (template_id, version_no),
    CONSTRAINT fk_etv_template FOREIGN KEY (template_id) REFERENCES evaluation_templates (id)
);

CREATE TABLE evaluation_criteria (
    id          UUID          NOT NULL,
    version_id  UUID          NOT NULL,
    name        VARCHAR(160)  NOT NULL,
    description TEXT,
    weight      NUMERIC(5,2)  NOT NULL,
    max_score   NUMERIC(6,2),
    sort_order  INT,
    CONSTRAINT pk_evaluation_criteria PRIMARY KEY (id),
    CONSTRAINT ck_ec_weight CHECK (weight >= 0 AND weight <= 100),
    CONSTRAINT fk_ec_version FOREIGN KEY (version_id) REFERENCES evaluation_template_versions (id) ON DELETE CASCADE
);
CREATE INDEX idx_ec_version ON evaluation_criteria (version_id);

CREATE TABLE judge_evaluations (                     -- DB-09: one per judge+submission+round
    id                UUID        NOT NULL,
    judge_id          UUID        NOT NULL,           -- REF → judges
    assignment_id     UUID        NOT NULL,           -- REF → judge_assignments
    submission_id     UUID        NOT NULL,           -- REF → submissions
    rubric_version_id UUID        NOT NULL,
    competition_id    UUID        NOT NULL,           -- REF → competitions
    category_id       UUID        NOT NULL,           -- REF → competition_categories
    round_id          UUID        NOT NULL,           -- REF → competition_rounds
    status            VARCHAR(20) NOT NULL,
    submitted_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_judge_evaluations PRIMARY KEY (id),
    CONSTRAINT uq_jev_judge_sub_round UNIQUE (judge_id, submission_id, round_id),
    CONSTRAINT fk_jev_rubric_version FOREIGN KEY (rubric_version_id) REFERENCES evaluation_template_versions (id)
);
CREATE INDEX idx_jev_submission ON judge_evaluations (submission_id);
CREATE INDEX idx_jev_assignment ON judge_evaluations (assignment_id);

CREATE TABLE judge_evaluation_criterion_scores (
    evaluation_id UUID         NOT NULL,
    criterion_id  UUID         NOT NULL,
    score         NUMERIC(8,3) NOT NULL,
    comment       TEXT,
    CONSTRAINT pk_jecs PRIMARY KEY (evaluation_id, criterion_id),
    CONSTRAINT fk_jecs_eval FOREIGN KEY (evaluation_id) REFERENCES judge_evaluations (id),
    CONSTRAINT fk_jecs_criterion FOREIGN KEY (criterion_id) REFERENCES evaluation_criteria (id)
);
