-- V1.1  M03 Talent Skills

CREATE TABLE talent_skills (
    id              UUID         NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL,
    parent_skill_id UUID,
    display_order   INT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_talent_skills PRIMARY KEY (id),
    CONSTRAINT uq_talent_skills_name UNIQUE (name),
    CONSTRAINT ck_ts_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT fk_ts_parent FOREIGN KEY (parent_skill_id) REFERENCES talent_skills (id)
);

CREATE TABLE skill_proficiencies (
    id       UUID        NOT NULL,
    code     VARCHAR(40) NOT NULL,
    label    VARCHAR(80) NOT NULL,
    ordinal  INT         NOT NULL,
    CONSTRAINT pk_skill_proficiencies PRIMARY KEY (id),
    CONSTRAINT uq_skill_proficiencies_code UNIQUE (code)
);

CREATE TABLE user_talent_skills (
    user_id        UUID        NOT NULL,
    skill_id       UUID        NOT NULL,
    proficiency_id UUID,
    added_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_talent_skills PRIMARY KEY (user_id, skill_id),
    CONSTRAINT fk_uts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_uts_skill FOREIGN KEY (skill_id) REFERENCES talent_skills (id),
    CONSTRAINT fk_uts_proficiency FOREIGN KEY (proficiency_id) REFERENCES skill_proficiencies (id)
);
CREATE INDEX idx_uts_skill ON user_talent_skills (skill_id);
