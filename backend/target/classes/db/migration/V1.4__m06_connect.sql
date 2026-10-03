-- V1.4  M06 StarMitra Connect
-- project_id, sender/member user_ids follow physical design's FK classification
-- (users = foundation table, physical FK; project_id = REF, no FK).

CREATE TABLE conversations (
    id         UUID        NOT NULL,
    type       VARCHAR(20) NOT NULL,
    project_id UUID,                          -- REF → creative_rooms (cross-module)
    status     VARCHAR(20) NOT NULL,
    created_by UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT ck_conv_type CHECK (type IN ('ONE_TO_ONE','GROUP','PROJECT')),
    CONSTRAINT fk_conv_creator FOREIGN KEY (created_by) REFERENCES users (id)
);
CREATE INDEX idx_conv_project ON conversations (project_id);

CREATE TABLE conversation_members (
    conversation_id UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    role            VARCHAR(20),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at         TIMESTAMPTZ,
    CONSTRAINT pk_conversation_members PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cm_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_cm_user ON conversation_members (user_id);

CREATE TABLE messages (
    id              UUID        NOT NULL,
    conversation_id UUID        NOT NULL,
    sender_id       UUID        NOT NULL,
    sequence        BIGINT      NOT NULL,
    body            TEXT,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT uq_messages_conv_seq UNIQUE (conversation_id, sequence),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE TABLE message_receipts (
    message_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    status     VARCHAR(10) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_message_receipts PRIMARY KEY (message_id, user_id),
    CONSTRAINT ck_receipt_status CHECK (status IN ('DELIVERED','READ')),
    CONSTRAINT fk_mr_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE message_attachments (
    id         UUID NOT NULL,
    message_id UUID NOT NULL,
    media_id   UUID NOT NULL,                 -- REF → media_assets (cross-module)
    sort_order INT,
    CONSTRAINT pk_message_attachments PRIMARY KEY (id),
    CONSTRAINT fk_ma_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
);
CREATE INDEX idx_mat_message ON message_attachments (message_id);

CREATE TABLE user_blocks (
    blocker_id UUID        NOT NULL,
    blocked_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_blocks PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT ck_ub_not_self CHECK (blocker_id <> blocked_id),
    CONSTRAINT fk_ub_blocker FOREIGN KEY (blocker_id) REFERENCES users (id),
    CONSTRAINT fk_ub_blocked FOREIGN KEY (blocked_id) REFERENCES users (id)
);
CREATE INDEX idx_ub_blocked ON user_blocks (blocked_id);

CREATE TABLE message_idempotency (
    client_message_id VARCHAR(128) NOT NULL,
    message_id        UUID         NOT NULL,
    user_id           UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_message_idempotency PRIMARY KEY (client_message_id)
);
