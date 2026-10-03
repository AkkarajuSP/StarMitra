-- V1.6  M08 Portfolio
-- skill_id / media_id / project_credit_id are logical cross-module refs — no FK.

CREATE TABLE portfolios (
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    title      VARCHAR(160),
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_portfolios PRIMARY KEY (id),
    CONSTRAINT uq_portfolios_user UNIQUE (user_id),   -- DB-04: one portfolio per user
    CONSTRAINT fk_pf_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE portfolio_items (
    id           UUID         NOT NULL,
    portfolio_id UUID         NOT NULL,
    title        VARCHAR(160) NOT NULL,
    description  TEXT,
    skill_id     UUID,                          -- REF → talent_skills
    visibility   VARCHAR(20)  NOT NULL,
    sort_order   INT,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_portfolio_items PRIMARY KEY (id),
    CONSTRAINT fk_pi_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);
CREATE INDEX idx_pfi_portfolio ON portfolio_items (portfolio_id);
CREATE INDEX idx_pfi_visibility ON portfolio_items (visibility, status);

CREATE TABLE portfolio_item_media (
    item_id    UUID NOT NULL,
    media_id   UUID NOT NULL,                   -- REF → media_assets
    sort_order INT,
    CONSTRAINT pk_portfolio_item_media PRIMARY KEY (item_id, media_id),
    CONSTRAINT fk_pim_item FOREIGN KEY (item_id) REFERENCES portfolio_items (id) ON DELETE CASCADE
);
CREATE INDEX idx_pim_media ON portfolio_item_media (media_id);

CREATE TABLE portfolio_item_contributions (
    id                UUID NOT NULL,
    item_id           UUID NOT NULL,
    project_credit_id UUID NOT NULL,            -- REF → project_credits
    CONSTRAINT pk_portfolio_item_contributions PRIMARY KEY (id),
    CONSTRAINT uq_pic_item_credit UNIQUE (item_id, project_credit_id),
    CONSTRAINT fk_pic_item FOREIGN KEY (item_id) REFERENCES portfolio_items (id) ON DELETE CASCADE
);
