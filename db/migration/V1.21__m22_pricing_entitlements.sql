-- V1.21  M22 Pricing & Entitlements (plan catalog + entitlement config + user subscriptions)
-- Pricing/entitlement truth lives here only; business modules resolve via the
-- EntitlementContract seam — no plan rules inside other modules. Payment rails
-- are deliberately absent (no provider tables, no transactions, no webhooks).

CREATE TABLE plans (
    id             UUID          NOT NULL,
    code           VARCHAR(40)   NOT NULL,
    display_name   VARCHAR(80)   NOT NULL,
    description    TEXT,
    plan_type      VARCHAR(30)   NOT NULL,
    price          NUMERIC(12,2) NOT NULL,
    currency       CHAR(3)       NOT NULL DEFAULT 'INR',
    billing_period VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    sort_order     INT           NOT NULL DEFAULT 0,
    effective_from TIMESTAMPTZ,
    effective_to   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_plans PRIMARY KEY (id),
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT ck_plans_type CHECK (plan_type IN ('SUBSCRIPTION','EVENT_PACKAGE')),
    CONSTRAINT ck_plans_period CHECK (billing_period IN ('NONE','MONTH','EVENT')),
    CONSTRAINT ck_plans_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT ck_plans_price CHECK (price >= 0),
    CONSTRAINT ck_plans_effective CHECK (effective_to IS NULL OR effective_from IS NULL
                                          OR effective_to > effective_from)
);

CREATE TABLE entitlements (
    code         VARCHAR(60)  NOT NULL,     -- e.g. portfolio.items, competition.create
    display_name VARCHAR(120) NOT NULL,
    description  TEXT,
    value_kind   VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_entitlements PRIMARY KEY (code),
    CONSTRAINT ck_ent_kind CHECK (value_kind IN ('BOOLEAN','INTEGER','ENUM')),
    CONSTRAINT ck_ent_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE plan_entitlements (
    plan_id          UUID         NOT NULL,
    entitlement_code VARCHAR(60)  NOT NULL,
    value            VARCHAR(120) NOT NULL,  -- interpreted via entitlements.value_kind
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_plan_entitlements PRIMARY KEY (plan_id, entitlement_code),
    CONSTRAINT fk_pe_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_entitlement FOREIGN KEY (entitlement_code) REFERENCES entitlements (code)
);

-- One ACTIVE row per user enforced by partial unique index below.
CREATE TABLE user_subscriptions (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    plan_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    source     VARCHAR(30) NOT NULL,
    starts_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_us_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_us_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT ck_us_status CHECK (status IN ('ACTIVE','EXPIRED','CANCELLED','PENDING_PAYMENT')),
    CONSTRAINT ck_us_source CHECK (source IN ('SYSTEM_DEFAULT','USER_SELECTION','ADMIN_GRANT','PAYMENT')),
    CONSTRAINT ck_us_dates CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX idx_us_user ON user_subscriptions (user_id);
CREATE UNIQUE INDEX uq_us_current ON user_subscriptions (user_id) WHERE status = 'ACTIVE';

-- Generic usage counters: owning modules report consumption via the contract;
-- M22 never reaches into their tables to count.
CREATE TABLE entitlement_usage (
    user_id          UUID        NOT NULL,
    entitlement_code VARCHAR(60) NOT NULL,
    used             BIGINT      NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_entitlement_usage PRIMARY KEY (user_id, entitlement_code),
    CONSTRAINT fk_eu_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_eu_entitlement FOREIGN KEY (entitlement_code) REFERENCES entitlements (code),
    CONSTRAINT ck_eu_used CHECK (used >= 0)
);

-- ---------------------------------------------------------------------------
-- Reference data (deterministic IDs; idempotent seeds — same convention as V1.20)
-- ---------------------------------------------------------------------------

INSERT INTO plans (id, code, display_name, description, plan_type, price, currency,
                   billing_period, status, sort_order) VALUES
    ('00000000-0000-7000-8000-00000000a001', 'FREE',
     'Free', 'Start free. Showcase your talent. Grow your creative presence.',
     'SUBSCRIPTION', 0.00, 'INR', 'NONE', 'ACTIVE', 1),
    ('00000000-0000-7000-8000-00000000a002', 'CREATOR',
     'Creator', 'For creators building a serious portfolio and presence.',
     'SUBSCRIPTION', 99.00, 'INR', 'MONTH', 'ACTIVE', 2),
    ('00000000-0000-7000-8000-00000000a003', 'CREATOR_PRO',
     'Creator Pro', 'For established creators who need advanced analytics and reach.',
     'SUBSCRIPTION', 199.00, 'INR', 'MONTH', 'ACTIVE', 3),
    ('00000000-0000-7000-8000-00000000a004', 'COMPETITION_ORGANIZER',
     'Competition Organizer', 'Run competitions — categories, rounds, judges, rubrics, voting, leaderboards. Priced per event.',
     'EVENT_PACKAGE', 999.00, 'INR', 'EVENT', 'ACTIVE', 4)
ON CONFLICT (code) DO NOTHING;

INSERT INTO entitlements (code, display_name, description, value_kind) VALUES
    ('profile',               'Profile',                  'Public creator profile',                              'BOOLEAN'),
    ('portfolio.items',       'Portfolio items',          'Maximum portfolio items',                             'INTEGER'),
    ('media.uploads',         'Media uploads',            'Maximum stored media assets',                         'INTEGER'),
    ('creative_rooms.active', 'Active creative rooms',    'Maximum concurrently active creative rooms',          'INTEGER'),
    ('analytics.tier',        'Analytics tier',           'BASIC | ENHANCED | ADVANCED',                         'ENUM'),
    ('discovery.visibility',  'Discovery visibility',     'STANDARD | ENHANCED',                                 'ENUM'),
    ('competition.enter',     'Competition entry',        'Enter competitions as a participant',                 'BOOLEAN'),
    ('competition.create',    'Competition creation',     'Create and run competitions (organizer capability)',  'BOOLEAN')
ON CONFLICT (code) DO NOTHING;

INSERT INTO plan_entitlements (plan_id, entitlement_code, value) VALUES
    -- FREE
    ('00000000-0000-7000-8000-00000000a001', 'profile',               'true'),
    ('00000000-0000-7000-8000-00000000a001', 'portfolio.items',       '10'),
    ('00000000-0000-7000-8000-00000000a001', 'media.uploads',         '25'),
    ('00000000-0000-7000-8000-00000000a001', 'creative_rooms.active', '3'),
    ('00000000-0000-7000-8000-00000000a001', 'analytics.tier',        'BASIC'),
    ('00000000-0000-7000-8000-00000000a001', 'discovery.visibility',  'STANDARD'),
    ('00000000-0000-7000-8000-00000000a001', 'competition.enter',     'true'),
    -- CREATOR
    ('00000000-0000-7000-8000-00000000a002', 'profile',               'true'),
    ('00000000-0000-7000-8000-00000000a002', 'portfolio.items',       '50'),
    ('00000000-0000-7000-8000-00000000a002', 'media.uploads',         '100'),
    ('00000000-0000-7000-8000-00000000a002', 'creative_rooms.active', '15'),
    ('00000000-0000-7000-8000-00000000a002', 'analytics.tier',        'ENHANCED'),
    ('00000000-0000-7000-8000-00000000a002', 'discovery.visibility',  'ENHANCED'),
    ('00000000-0000-7000-8000-00000000a002', 'competition.enter',     'true'),
    -- CREATOR_PRO
    ('00000000-0000-7000-8000-00000000a003', 'profile',               'true'),
    ('00000000-0000-7000-8000-00000000a003', 'portfolio.items',       '200'),
    ('00000000-0000-7000-8000-00000000a003', 'media.uploads',         '500'),
    ('00000000-0000-7000-8000-00000000a003', 'creative_rooms.active', '50'),
    ('00000000-0000-7000-8000-00000000a003', 'analytics.tier',        'ADVANCED'),
    ('00000000-0000-7000-8000-00000000a003', 'discovery.visibility',  'ENHANCED'),
    ('00000000-0000-7000-8000-00000000a003', 'competition.enter',     'true'),
    -- COMPETITION_ORGANIZER — priced per event, not a monthly creator seat
    ('00000000-0000-7000-8000-00000000a004', 'competition.create',    'true'),
    ('00000000-0000-7000-8000-00000000a004', 'analytics.tier',        'ADVANCED')
ON CONFLICT (plan_id, entitlement_code) DO NOTHING;
