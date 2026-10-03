-- V1.2  M04 Media Management
-- owner_user_id is a logical cross-module ref to users — no FK by design.

CREATE TABLE media_assets (
    id               UUID         NOT NULL,
    owner_user_id    UUID         NOT NULL,          -- REF → users (cross-module, no FK)
    object_key       VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    media_type       VARCHAR(20)  NOT NULL,
    mime_type        VARCHAR(127) NOT NULL,
    size_bytes       BIGINT,
    checksum         VARCHAR(128),
    upload_state     VARCHAR(20)  NOT NULL,
    processing_state VARCHAR(20)  NOT NULL,
    moderation_state VARCHAR(20)  NOT NULL,
    visibility       VARCHAR(30)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_media_assets PRIMARY KEY (id),
    CONSTRAINT uq_media_assets_object_key UNIQUE (object_key),
    CONSTRAINT ck_ma_upload CHECK (upload_state IN ('INITIATED','UPLOADED','VERIFIED','FAILED')),
    CONSTRAINT ck_ma_processing CHECK (processing_state IN ('PENDING','PROCESSING','COMPLETED','FAILED','NOT_REQUIRED')),
    CONSTRAINT ck_ma_moderation CHECK (moderation_state IN ('PENDING','APPROVED','REJECTED','RESTRICTED')),
    CONSTRAINT ck_ma_visibility CHECK (visibility IN ('PUBLIC','FOLLOWERS','COLLABORATION_ONLY','PRIVATE'))
);
CREATE INDEX idx_ma_owner ON media_assets (owner_user_id);
CREATE INDEX idx_ma_visibility ON media_assets (visibility, moderation_state);

CREATE TABLE media_variants (
    id             UUID         NOT NULL,
    media_asset_id UUID         NOT NULL,
    variant_type   VARCHAR(40)  NOT NULL,
    object_key     VARCHAR(512) NOT NULL,
    width          INT,
    height         INT,
    format         VARCHAR(20),
    size_bytes     BIGINT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_media_variants PRIMARY KEY (id),
    CONSTRAINT uq_media_variants_object_key UNIQUE (object_key),
    CONSTRAINT fk_mv_asset FOREIGN KEY (media_asset_id) REFERENCES media_assets (id)
);
CREATE INDEX idx_mv_asset ON media_variants (media_asset_id);
