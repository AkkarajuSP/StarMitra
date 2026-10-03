# StarMitra — Analytics Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review | **Decision status:** under review as **OD-13** in the [Decision Register](../ADR/ARCHITECTURE-DECISION-REGISTER.md)

## 1. Scope — FRS-Required Analytics `[FRS §29]`

Registered users, active creators, skill/category popularity, uploads/engagement, competition participation, voting activity, judge completion, per-criterion score averages, round progression, collaboration statistics, top-talent metrics — all operational aggregates of system-of-record data. Product-level funnels/retention are **inference, not FRS** — deferred.

## 2. Layer Boundaries

| Layer | Store | Rule |
|-------|-------|------|
| Transactional data | PostgreSQL | Authoritative |
| Reporting/read models | PG views/materialized/rollup tables | Derived, refreshable — never authoritative |
| Analytics events | PG table (if adopted later) | Telemetry, not facts |
| Audit `[FRS §30]` | PostgreSQL AuditLog | Separate compliance trail |
| Competition results | PostgreSQL | **Analytics never overrides authoritative records** |

## 3. MVP Approach `[Proposed]`

D15 module: direct indexed PG queries for light aggregates; scheduled rollup jobs for heavy/recurring aggregates (leaderboards, per-criterion averages); admin dashboards via synchronous queries + periodic aggregation. Read replica only if measured contention demands (OD-12 trigger). No warehouse, no third-party analytics, no event streaming at MVP.

## 4. Privacy & Governance

Aggregate-only analytics surfaces; private user content never an analytics input; admin-role authz on reporting APIs; judge-identity scoping per `[FRS §19]`; retention/anonymization follow product policy (open); legal/consent review flagged for any future event tracking.

## 5. Deferred — each requires a separate decision

Separate analytics datastore, warehouse, event streaming/CDC, dedicated pipeline, BI platform, real-time analytics infra, and all third-party tools (GA/Mixpanel/Amplitude/PostHog/Segment/Snowflake/BigQuery/Redshift/ClickHouse). Triggers documented in register OD-13 §11.
