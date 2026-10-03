-- V1.3  M02 User Profile  (after M01 users + M04 media for REF documentation order)
-- avatar_media_id / banner_media_id are logical cross-module refs to media_assets — no FK.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE user_profiles (
    id               UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    display_name     VARCHAR(120) NOT NULL,
    bio              TEXT,
    location         VARCHAR(120),
    avatar_media_id  UUID,                          -- REF → media_assets
    banner_media_id  UUID,                          -- REF → media_assets
    visibility_state VARCHAR(30)  NOT NULL,
    completion_score SMALLINT,
    search_vector    TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(display_name,'') || ' ' || coalesce(bio,''))
    ) STORED,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_up_visibility CHECK (visibility_state IN ('PUBLIC','FOLLOWERS','COLLABORATION_ONLY','PRIVATE'))
);

CREATE INDEX idx_up_search_vector ON user_profiles USING GIN (search_vector);
CREATE INDEX idx_up_display_name_trgm ON user_profiles USING GIN (display_name gin_trgm_ops);
CREATE INDEX idx_up_visibility ON user_profiles (visibility_state);
