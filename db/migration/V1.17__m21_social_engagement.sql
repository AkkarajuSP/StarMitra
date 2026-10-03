-- V1.17  M21 Social Engagement  (DB-01 targets MEDIA/PORTFOLIO · DB-05 create+delete · DB-06 user-only)
-- target_id is a polymorphic typed ref — no FK by design.

CREATE TABLE follows (
    follower_id UUID        NOT NULL,
    followee_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_follows PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT ck_follows_not_self CHECK (follower_id <> followee_id),
    CONSTRAINT fk_fol_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_fol_followee FOREIGN KEY (followee_id) REFERENCES users (id)
);
CREATE INDEX idx_fol_followee ON follows (followee_id);

CREATE TABLE likes (
    id          UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_likes PRIMARY KEY (id),
    CONSTRAINT ck_likes_target CHECK (target_type IN ('MEDIA','PORTFOLIO')),
    CONSTRAINT uq_likes_unique UNIQUE (user_id, target_type, target_id),
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_likes_target ON likes (target_type, target_id);

CREATE TABLE comments (
    id          UUID        NOT NULL,
    author_id   UUID        NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID        NOT NULL,
    body        TEXT        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT ck_comments_target CHECK (target_type IN ('MEDIA','PORTFOLIO')),
    CONSTRAINT ck_comments_status CHECK (status IN ('ACTIVE','REMOVED')),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);  -- delete is soft via status; no edited_at/parent_comment_id (DB-05)
CREATE INDEX idx_comments_target ON comments (target_type, target_id, created_at);
CREATE INDEX idx_comments_author ON comments (author_id);

CREATE TABLE engagement_counters (                  -- derived/rebuildable projection
    target_type   VARCHAR(20) NOT NULL,
    target_id     UUID        NOT NULL,
    follow_count  BIGINT      NOT NULL DEFAULT 0,
    like_count    BIGINT      NOT NULL DEFAULT 0,
    comment_count BIGINT      NOT NULL DEFAULT 0,
    refreshed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_engagement_counters PRIMARY KEY (target_type, target_id)
);
