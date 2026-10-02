# StarMitra — System Context

**Parent:** [Architecture Baseline v1.0](STARMITRA-ARCHITECTURE-BASELINE-v1.0.md) | **Status:** Draft for review

## 1. System Purpose

`[FRS §1]` StarMitra is a mobile-first talent discovery, competition, collaboration and entertainment-content platform. Users showcase creative work, discover creators, collaborate through Creative Rooms, enter competitions, receive audience votes and judge evaluations, and progress toward production opportunities.

Tagline: **Passion to Perform**

## 2. Actors

| Actor | Description | FRS |
|-------|-------------|-----|
| Visitor | Unauthenticated public web user browsing discovery/public content | §5 |
| Audience / User | Authenticated user: browse, follow, like, comment, vote where eligible, report | §6 |
| Creator / Talent | User with creator capabilities: profile + multiple skills, uploads, competition entries, collaboration | §6 |
| Project Owner | Creator who initiates/owns a Creative Room or project submission | §13, §17 |
| Judge | System role holder evaluating assigned submissions via dynamic rubrics | §6, §19–21 |
| Admin | Manages users, skills, competitions, rounds, rubrics, moderation, reports, config | §6, §27 |
| Super Admin | Admin plus system-level configuration | §6 |

Note: these are **system roles** (authorization). Creative disciplines (Singer, Actor, Director, …) are talent skills, not actors/roles `[PD-02][BR-02]`.

## 3. Channels

`[FRS §5]`

| Channel | Primary users | Key capabilities |
|---------|---------------|------------------|
| Mobile app | Audience, Creators | Create, upload, discover, engage, message, participate |
| Public web | All/visitors | Landing, discovery, selected public content |
| Admin web | Admin, Super Admin | Configuration, moderation, competition ops, users, analytics |
| Judge web | Judges | Assigned submissions, dynamic evaluation forms, scoring, comments |

## 4. Context Diagram

```text
                        ┌─────────────┐
                        │  Visitors   │
                        └──────┬──────┘
                               │ public web
        ┌─────────────┐        │
        │  Audience / │   ┌────▼─────────────────────────────┐
        │  Creators   ├───►│                                  │
        └─────────────┘    │                                  │
        ┌─────────────┐    │         STARMITRA                │
        │   Judges    ├────►  (mobile + web API backend)      │
        └─────────────┘    │                                  │
        ┌─────────────┐    │                                  │
        │ Admins /    ├────►                                  │
        │ Super Admin │    └──────┬───────┬───────┬───────┬───┘
        └─────────────┘           │       │       │       │
                          ┌───────▼──┐ ┌──▼─────┐ │       │
                          │ Push/    │ │ Media  │ │       │
                          │ Email/SMS│ │ storage│ │       │
                          │ providers│ │ + CDN  │ │       │
                          └──────────┘ └────────┘ │       │
                                       ┌──────────▼─┐ ┌───▼─────────┐
                                       │ Transcoding │ │ AuthN/OTP   │
                                       │ (media)     │ │ provider    │
                                       └─────────────┘ └─────────────┘
                          [All external systems = Proposed/Open]
```

## 5. External Systems

`[Proposed]` — all subject to ADR approval; none mandated by the FRS.

| System | Purpose | Status |
|--------|---------|--------|
| OTP/SMS provider | Registration & login verification `[FRS §8]` | Open (OD-5, OD-7) |
| Email provider | Notifications, account flows `[FRS §25]` | Open (OD-7) |
| Push notification provider | Mobile notifications `[FRS §25]` | Open (OD-7) |
| Object storage | Media binaries `[FRS §10][§36]` | Open (OD-6) |
| CDN | Media delivery at scale `[FRS §36]` | Open (OD-6) |
| Media transcoding | Format normalization, thumbnails, previews | Open (OD-6) |
| Identity provider / social login | Future option `[FRS §8 optional social login]` | Future — out of MVP |

## 6. Boundary Rules

- Everything inside the StarMitra boundary trusts the five-concept separation (`[PD-01..03]`): the API layer and authorization middleware enforce it at every entry point.
- External providers are always behind adapter interfaces — no provider SDK types cross domain boundaries `[Proposed]`.
- Admin/Judge web use the same backend but **separate API surface authorization** — a judge token never unlocks admin endpoints and vice versa `[FRS §6][§19]`.
