-- V1.0  M01 Authentication & Identity
-- UUID PKs are application-generated (UUIDv7) — no DB default by design.

CREATE TABLE users (
    id          UUID          NOT NULL,
    email       VARCHAR(320)  NOT NULL,
    phone       VARCHAR(32),
    status      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','SUSPENDED','BLOCKED','DEACTIVATED'))
);

CREATE TABLE system_roles (
    id          UUID          NOT NULL,
    name        VARCHAR(50)   NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_system_roles PRIMARY KEY (id),
    CONSTRAINT uq_system_roles_name UNIQUE (name)
);

CREATE TABLE user_system_roles (
    user_id     UUID          NOT NULL,
    role_id     UUID          NOT NULL,
    assigned_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_system_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_usr_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_usr_role FOREIGN KEY (role_id) REFERENCES system_roles (id)
);
CREATE INDEX idx_usr_role ON user_system_roles (role_id);

CREATE TABLE refresh_tokens (
    id              UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    family_id       UUID         NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by     UUID,
    reuse_detected  BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_rt_user ON refresh_tokens (user_id);

CREATE TABLE otp_challenges (
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    otp_hash     VARCHAR(128) NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    max_attempts INT          NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_otp_challenges PRIMARY KEY (id),
    CONSTRAINT fk_oc_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_oc_attempts CHECK (attempts >= 0 AND attempts <= max_attempts)
);
CREATE INDEX idx_oc_user ON otp_challenges (user_id);
CREATE INDEX idx_oc_expires ON otp_challenges (expires_at);

CREATE TABLE authentication_audit_events (
    id         UUID         NOT NULL,
    user_id    UUID,
    event_type VARCHAR(50)  NOT NULL,
    metadata   JSONB,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_auth_audit_events PRIMARY KEY (id)
);  -- append-only: no updated_at
CREATE INDEX idx_aae_user ON authentication_audit_events (user_id);
CREATE INDEX idx_aae_created ON authentication_audit_events (created_at);
