-- V1.5  M07 Creative Rooms
-- skill_id / media_id are logical cross-module refs — no FK by design.

CREATE TABLE creative_rooms (
    id          UUID         NOT NULL,
    owner_id    UUID         NOT NULL,
    name        VARCHAR(160) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    visibility  VARCHAR(20)  NOT NULL,
    version     INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_creative_rooms PRIMARY KEY (id),
    CONSTRAINT fk_cr_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
CREATE INDEX idx_cr_owner ON creative_rooms (owner_id);
CREATE INDEX idx_cr_status ON creative_rooms (status, visibility);

CREATE TABLE project_members (
    room_id   UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    status    VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at   TIMESTAMPTZ,
    CONSTRAINT pk_project_members PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_pm_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id),
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_pm_user ON project_members (user_id);

CREATE TABLE project_contribution_roles (
    id             UUID        NOT NULL,
    room_id        UUID        NOT NULL,
    member_user_id UUID        NOT NULL,
    role_name      VARCHAR(80) NOT NULL,
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at     TIMESTAMPTZ,
    CONSTRAINT pk_project_contribution_roles PRIMARY KEY (id),
    CONSTRAINT uq_pcr_room_member_role UNIQUE (room_id, member_user_id, role_name),
    CONSTRAINT fk_pcr_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id)
);
CREATE INDEX idx_pcr_room ON project_contribution_roles (room_id);

CREATE TABLE required_skills (
    room_id  UUID NOT NULL,
    skill_id UUID NOT NULL,                   -- REF → talent_skills
    CONSTRAINT pk_required_skills PRIMARY KEY (room_id, skill_id),
    CONSTRAINT fk_rs_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id)
);
CREATE INDEX idx_rs_skill ON required_skills (skill_id);

CREATE TABLE project_invitations (
    id         UUID         NOT NULL,
    room_id    UUID         NOT NULL,
    inviter_id UUID         NOT NULL,
    invitee_id UUID         NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_project_invitations PRIMARY KEY (id),
    CONSTRAINT ck_pi_status CHECK (status IN ('PENDING','ACCEPTED','DECLINED','WITHDRAWN','EXPIRED')),
    CONSTRAINT fk_pi_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id),
    CONSTRAINT fk_pi_inviter FOREIGN KEY (inviter_id) REFERENCES users (id),
    CONSTRAINT fk_pi_invitee FOREIGN KEY (invitee_id) REFERENCES users (id)
);
CREATE INDEX idx_pi_invitee ON project_invitations (invitee_id);
CREATE UNIQUE INDEX uq_pi_pending ON project_invitations (room_id, invitee_id) WHERE status = 'PENDING';

CREATE TABLE project_tasks (
    id                 UUID         NOT NULL,
    room_id            UUID         NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    assignee_member_id UUID,
    status             VARCHAR(20)  NOT NULL,
    due_date           DATE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_project_tasks PRIMARY KEY (id),
    CONSTRAINT fk_pt_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id)
);
CREATE INDEX idx_pt_room ON project_tasks (room_id);
CREATE INDEX idx_pt_assignee ON project_tasks (assignee_member_id);

CREATE TABLE project_assets (
    id       UUID        NOT NULL,
    room_id  UUID        NOT NULL,
    media_id UUID        NOT NULL,            -- REF → media_assets
    role     VARCHAR(40),
    CONSTRAINT pk_project_assets PRIMARY KEY (id),
    CONSTRAINT uq_pa_room_media UNIQUE (room_id, media_id),
    CONSTRAINT fk_pa_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id)
);

CREATE TABLE final_outputs (
    id           UUID        NOT NULL,
    room_id      UUID        NOT NULL,
    media_id     UUID        NOT NULL,        -- REF → media_assets
    finalized_at TIMESTAMPTZ NOT NULL,
    output_type  VARCHAR(40),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_final_outputs PRIMARY KEY (id),
    CONSTRAINT fk_fo_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id)
);
CREATE INDEX idx_fo_room ON final_outputs (room_id);

CREATE TABLE project_credits (
    id                    UUID         NOT NULL,
    room_id               UUID         NOT NULL,
    member_user_id        UUID         NOT NULL,
    contribution_role_id  UUID         NOT NULL,
    credit_label          VARCHAR(160) NOT NULL,
    verified              BOOLEAN      NOT NULL DEFAULT false,
    verified_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_project_credits PRIMARY KEY (id),
    CONSTRAINT fk_pc_room FOREIGN KEY (room_id) REFERENCES creative_rooms (id),
    CONSTRAINT fk_pc_user FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_pc_role FOREIGN KEY (contribution_role_id) REFERENCES project_contribution_roles (id)
);
CREATE INDEX idx_pc_room ON project_credits (room_id);
CREATE INDEX idx_pc_user ON project_credits (member_user_id);
