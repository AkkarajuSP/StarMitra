# StarMitra Core Product Model

**Baseline:** StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model

This document defines the mandatory conceptual separation at the heart of the StarMitra domain model. These concepts are distinct and MUST be modeled separately in every layer (domain, database, API, authorization).

## The Five Core Concepts

### 1. User

A registered person on the StarMitra platform. The User is the identity anchor for everything else: profile, portfolio, talent skills, project memberships, and competition participation.

### 2. System Role

A **platform authorization role** that determines what a user is permitted to do in the application (e.g., Member, Judge, Moderator, Admin). System roles drive access control (RBAC) and nothing else. They are assigned administratively, not earned through talent.

### 3. Talent Skill

A **creative capability** a user claims and showcases on their profile (e.g., Singer, Actor, Director, Dialogue Writer, Dancer, Musician). Talent skills are profile attributes used for discovery, portfolio organization, and matching. They carry **no authorization semantics whatsoever**.

### 4. Project Contribution Role

The **capacity in which a user contributes to a specific project** (e.g., Director on Project B, Dialogue Writer on Project C). It is contextual to a single project membership — the same user may contribute to different projects in different capacities. A contribution role typically references a talent skill but is a distinct concept: it is a project-scoped assignment, not a profile claim and not a permission.

### 5. Competition Participation

The user's entry in a specific competition in a specific talent category (e.g., Competition A → Singer). Participation links a user, a competition, and the talent skill under which they compete.

## Multi-Talent Model

A single user may hold many talent skills:

```text
User: Priya
    ├── Singer
    ├── Actor
    ├── Director
    └── Dialogue Writer
```

The same user may participate in different capacities across contexts:

```text
Competition A  →  Singer
Project B      →  Director
Project C      →  Dialogue Writer
```

## Mandatory Data Model

### User scope

```text
User
   │
   ├── UserTalentSkill ──> TalentSkill
   │
   └── UserSystemRole ──> SystemRole
```

- `TalentSkill` — catalog of creative skills (Singer, Actor, Director, Writer, …). Data-driven; extendable without code changes.
- `SystemRole` — catalog of authorization roles (Member, Judge, Moderator, Admin, …). Used exclusively for access control.
- `UserTalentSkill` — join: which skills a user claims (with proficiency, verification status, etc. as needed).
- `UserSystemRole` — join: which authorization roles a user holds.

### Project scope

```text
Project
   │
   └── ProjectMember
           │
           └── ProjectContributionRole
```

- `ProjectMember` — a user's membership in a project.
- `ProjectContributionRole` — the capacity/capacities in which that member contributes.

## Cardinal Rule

> **Creative skills MUST NOT automatically grant system permissions.**

Do NOT model Singer, Actor, Director, Writer, or any other talent as an application authorization role. Being a "Director" on a project does not make a user a platform admin; being a "Judge" (a system role) is an authorization assignment, not a talent claim.

- Authorization decisions → consult `UserSystemRole` only.
- Profile, discovery, matching → consult `UserTalentSkill`.
- Project participation → consult `ProjectMember` / `ProjectContributionRole`.
- Competition entries → consult `Competition Participation`.

Any design that conflates these concepts is a deviation from the FRS baseline and requires explicit review and approval before implementation.
