# ADR-004 — Web Frontend: React + TypeScript + Vite SPA

| Field | Content |
|-------|---------|
| ADR | ADR-004 |
| Register entry | OD-04 — Web Frontend |
| Status | **ACCEPTED** |
| Date | 2026-10-03 |
| Baseline refs | §7 Application Architecture, §20 Decisions |
| Related | ADR-005 (mobile alignment), ADR-006 (auth/session), ADR-007 (API contract) |

## Decision

**React + TypeScript + Vite** single-page application — **one codebase** serving all four web surfaces (public, authenticated app, judge portal, admin portal) via role-gated route groups.

## Context

Four web surfaces with different characters `[FRS §5][§27][§28]`; mobile-first responsive mandatory `[§36]`; brand design system to implement; backend is Spring Boot REST/OpenAPI + WS.

## Alternatives Considered

- **Next.js (SSR-capable)** — strong alternative *if* public-discovery SEO is confirmed critical at MVP; adds server runtime + complexity for portals that don't need it.
- **Angular** — overkill for consumer surfaces; ecosystem split.
- **Vue/Nuxt** — capable; no advantage.
- **SvelteKit** — smallest ecosystem; team risk.

## Rationale

Simplest deployment (static assets/CDN — no SSR server needed); fastest dev loop; all four surfaces fit SPA well; three of four need no SEO anyway.

## Guardrails / Constraints (binding)

1. React + TypeScript + Vite is the approved web frontend baseline.
2. **One** web application/codebase serves public, authenticated app, judge and admin experiences.
3. Route groups are organizational/access boundaries only — **backend authorization remains authoritative**.
4. **Mobile-first responsive design is mandatory.**
5. WebSocket integration remains isolated from core frontend domain/state logic.
6. Specific frontend libraries remain separate decisions/recommendations.
7. Authentication/session strategy remains deferred to ADR-006.
8. SEO/SSR remains an explicit open sub-decision.
9. Next.js, Redux, Zustand, Tailwind, or other libraries/frameworks are NOT automatically approved.

## Consequences

Loses built-in SSR — mitigable later if SEO is confirmed (public-site carve-out possible); gains simplest deployment aligned with monolith static serving.

## Deferred Items / Future Triggers

SEO/SSR carve-out if organic discovery is confirmed critical (product input); library choices (state, forms, styling, codegen tool) at implementation.

## Open Items

- SEO requirement at MVP — product input
- Session transport for web — resolved by ADR-006 (httpOnly cookies)
- Styling system — separate review
