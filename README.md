# StarMitra

**Passion to Perform**

StarMitra is a multi-talent / multi-skill platform where creative professionals can showcase their talents, build portfolios, connect and collaborate on projects, and participate in judged competitions with audience voting and structured evaluation.

A single user may hold multiple talent skills (e.g., Singer, Actor, Director, Dialogue Writer) and participate in different capacities across competitions and projects. Talent skills are profile attributes — they are not system permissions.

## Repository Purpose

This repository is the single source of truth for the StarMitra platform: requirements, product design, architecture, module specifications, API specifications, database design, source code, testing, deployment, security, and documentation.

The functional baseline is **StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model**. See [`01_Requirements/FRS/`](01_Requirements/FRS/README.md). Any functional deviation from this baseline must be explicitly reviewed and approved before implementation.

## High-Level Architecture

> Placeholder — architecture decisions are pending approval.
>
> Core modeling decisions already established (see [`03_Architecture/Architecture/STARMITRA-CORE-PRODUCT-MODEL.md`](03_Architecture/Architecture/STARMITRA-CORE-PRODUCT-MODEL.md)):
>
> - `User`, `System Role`, `Talent Skill`, `Project Contribution Role`, and `Competition Participation` are separate concepts.
> - Talent skills (Singer, Actor, Director, Writer, etc.) MUST NOT be modeled as application authorization roles.
> - Judge evaluation is data-driven and configurable; see [`03_Architecture/Architecture/JUDGE-RUBRIC-MODEL.md`](03_Architecture/Architecture/JUDGE-RUBRIC-MODEL.md).
>
> Technology stack, infrastructure, and deployment architecture are to be decided and recorded as ADRs in [`03_Architecture/ADR/`](03_Architecture/ADR/).

## Development Structure

All application source code lives under `07_Development/`:

```text
07_Development/
├── Backend/     # Server-side application code
├── Frontend/    # Web application code
├── Mobile/      # Mobile application code
└── Shared/      # Shared libraries, types, utilities
```

`04_Modules/` contains module-specific functional and design documentation only — it does not contain source code.

## Documentation Structure

| Directory | Contents |
|-----------|----------|
| `00_Project-Management/` | Decisions, meeting notes, project planning |
| `01_Requirements/` | FRS, BRD, user stories, business rules, acceptance criteria |
| `02_Product-Design/` | User journeys, wireframes, UI/UX, screen specifications |
| `03_Architecture/` | Architecture, ADRs, database, API, security, integrations |
| `04_Modules/` | Per-module functional/design documentation |
| `05_API-Specifications/` | OpenAPI and per-domain API specs |
| `06_Database/` | ERD, schema, migrations, data dictionary |
| `07_Development/` | Application source code (Backend, Frontend, Mobile, Shared) |
| `08_Testing/` | Test strategy, test cases, API/integration/UAT/regression testing, defects |
| `09_Deployment/` | Environments, Docker, CI/CD, cloud, deployment guides |
| `10_Security/` | Security requirements, RBAC, threat model, security testing |
| `11_Documentation/` | User, admin, judge guides and technical documentation |
| `99_Archive/` | Archived/superseded material |

## Development Environments

> To be defined. Environment specifications will be documented in [`09_Deployment/Environments/`](09_Deployment/Environments/).

## Branching Strategy

> To be finalized. Interim convention:
>
> - `main` — stable, reviewed baseline
> - `feature/<short-description>` — feature and setup work
> - `fix/<short-description>` — defect fixes
>
> All changes are reviewed before merging to `main`.

## Functional Baseline

- **FRS**: [`01_Requirements/FRS/StarMitra_FRS_v1.1_MultiTalent.docx`](01_Requirements/FRS/StarMitra_FRS_v1.1_MultiTalent.docx)

## Project Status

**Foundation phase** — repository structure and architecture foundation documents established. Module implementation has not started.

Delivery sequence:

```text
FRS → Product/Architecture Foundation → Module Design → Database/API Design → Implementation → Testing → UAT → Deployment
```
