# StarMitra — Observability Architecture

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. Requirement

`[FRS §36]` "Observability through application logs, metrics and alerts" plus backup/recovery for critical data. Auditability (`[FRS §30]`) is a separate, product-level concern — see §5.

## 2. Pillars `[Proposed]`

| Pillar | Approach |
|--------|----------|
| **Logs** | Structured (JSON), correlation ID per request propagated to events/jobs; no secrets/PII in log payloads |
| **Metrics** | RED per API surface (rate, errors, duration); job queue depth/age; media pipeline throughput; vote ingestion rate; notification delivery success rate |
| **Traces** | Distributed tracing across request → domain → job/event chain `[Proposed]`; essential for scoring-pipeline debugging |
| **Alerting** | SLO-driven alerts + business-flow watchdogs (below) |
| **Dashboards** | Ops health + competition-cycle views (live voting windows, judging progress) |

`[Open]` Tooling/vendor selection — defer to environment/cloud ADRs.

## 3. Business-Flow Watchdogs `[Proposed]`

StarMitra's risk concentrates in competition lifecycle. Alert on:

| Signal | Why |
|--------|-----|
| Scheduled round/voting/judging transitions not firing | Deadline integrity `[FRS §23]` |
| Score aggregation failures/staleness after round close | Results correctness `[FRS §22]` |
| Vote ingestion anomalies (spike/drop/pattern) | Abuse detection `[BR-14]` |
| Media processing backlog/failures | Submission deadlines `[FRS §16]` |
| Notification delivery failure rate | Judges must see assignments `[FRS §25]` |
| Audit write failures | Compliance `[FRS §30]` |

## 4. Job & Event Observability

`[Proposed]` Every scheduled/async job (media processing, score computation, notification dispatch, deadline transitions) exposes: last-run, success/fail, duration, backlog. Events carry correlation IDs; failed events land in a replayable dead-letter store.

## 5. Audit vs Observability — explicit distinction

| | AuditLog `[FRS §30]` | Telemetry `[FRS §36]` |
|---|---|---|
| Purpose | Business/legal traceability | Operational health |
| Audience | Admin, compliance, dispute resolution | Engineering/SRE |
| Retention | Long, per policy | Shorter, cost-driven |
| Mutability | **Append-only, tamper-evident** | Best-effort |
| Content | Actor, action, entity, before/after, authz context | Latencies, errors, volumes |

Audit writes are transactional with the business operation `[Proposed]` — an audit failure in a guarded path (override, evaluation submit, vote) should fail the operation, not log-and-continue.

## 6. Environments

`[Proposed]` Non-prod telemetry may use reduced retention/sampling; prod requires full fidelity during competition windows. Backup/recovery procedures per `[FRS §36]` detailed in `09_Deployment/` during deployment design.
