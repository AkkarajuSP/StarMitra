# StarMitra Brand Baseline v1.0

**Product:** StarMitra — *Passion to Perform*
**Status:** Draft for review — brand/design foundation only
**Scope:** Visual standards derived from the supplied official logo. No screens, no components, no implementation. Does not modify FRS v1.1 or architecture decisions.

## 1. Brand Identity

| Item | Value |
|------|-------|
| Product | StarMitra |
| Tagline | Passion to Perform |
| Positioning | Mobile-first talent discovery, creative showcase, competition, collaboration and entertainment-content platform `[FRS §1]` |
| Desired feel | Modern, premium, creative, accessible, energetic — a stage for multi-talented performers |

## 2. Logo

### 2.1 Supplied assets

Two image renders of the official logo were provided. They appear identical: the **full lockup** — emblem (navy circular field with gold star, white swoosh, lavender performer figure, gold star trail) + "StarMitra" wordmark ("Star" violet / "Mitra" ink) + coral tagline — on a light background.

| Asset | Type | Status |
|-------|------|--------|
| Full lockup (emblem + wordmark + tagline), light background | Raster render | Received in-conversation; **source file pending drop-in to `../Logo/`** |

### 2.2 Identifiable elements (from the render)

- **Emblem:** navy circular field, white curved swoosh, white-outlined gold five-point star, light-lavender human figure reaching upward, arc of small gold stars — conveys aspiration, performance, achievement.
- **Wordmark:** "Star" in violet, "Mitra" in near-black ink, bold rounded geometric sans.
- **Tagline:** lowercase "passion to perform," coral, light letter-spaced.

### 2.3 Usage rules

- **Never** modify, recolor, distort, stretch, crop, or redraw the logo. Use the supplied official asset; do not recreate it in CSS/SVG unless explicitly required.
- **Clear space:** preserve breathing room around the lockup — minimum clear space ≈ the height of the star in the emblem on all sides *(provisional, pending source artwork)*.
- **Light backgrounds:** supplied lockup is designed for light backgrounds (ink wordmark). On navy/dark surfaces it requires either a light container or an officially produced reversed variant — **no reversed variant was supplied; do not fabricate one.**
- **Dark backgrounds:** place the full lockup inside a white/light rounded container, or request a reversed variant from brand — do not recolor locally.
- **Minimum size:** emblem legible down to ~24–28px; below that, use emblem only (without tagline) once such a variant exists. Full lockup minimum width ≈ 120px *(provisional)*.
- **Incorrect usage** (enforce): no recoloring, no drop shadows, no outline effects, no rotation, no placing on busy imagery without a scrim/container, no separating and re-coloring "Star"/"Mitra" differently, no condensed/stretched rendering, no low-contrast placement.

### 2.4 Missing variants (request from brand)

| Needed variant | Reason |
|----------------|--------|
| Emblem-only mark (no wordmark) | App icon, favicon, avatar, compact headers |
| Reversed/light lockup | Navy/dark surfaces, splash screens |
| Tagline-free lockup | Small sizes where tagline is illegible |
| Source vector (SVG/EPS/PDF) | Canonical asset for all derivatives |
| Favicon/app-icon exports | Platform assets |

## 3. Color System

Full system: [`../Colors/STARMITRA-COLOR-SYSTEM.md`](../Colors/STARMITRA-COLOR-SYSTEM.md)

Summary — official palette extracted (est.): Navy `#1B2A6B`, Gold `#F6A938`, Star Yellow `#F9BE3C`, Violet `#7F4FE0`, Lavender `#D8CCF4`, Ink `#1B1C2E`, Coral `#EE6F77`, White `#FFFFFF`. Functional colors (success/warning/error/info) are **PROPOSED UI COLOR**, not brand colors.

## 4. Typography

Full proposal: [`../Typography/STARMITRA-TYPOGRAPHY.md`](../Typography/STARMITRA-TYPOGRAPHY.md) — **Status: PROPOSED** (no official font supplied). Recommendation: Poppins (headings) + Inter (UI/body).

## 5. UI/UX Direction

Mobile-first, modern, premium, creative, accessible. Emotional tone: the "stage" — navy as the stage night sky, gold for achievement/stardom, violet for creative energy. Restraint: gold and coral are accents, not fills.

## 6. Design System Principles

### 6.1 Layout

- Mobile-first breakpoints; design at 360px up, adapt to tablet/desktop.
- Consistent spacing scale (proposed: 4px base — 4, 8, 12, 16, 24, 32, 48).
- Clear visual hierarchy: one primary action per screen section.
- Touch targets ≥ 44×44px; keyboard-navigable equivalents on web.
- Generous white space; cards over heavy dividers.

### 6.2 Component inventory (concept only — do NOT implement)

Buttons (primary navy / secondary violet-outline / destructive), inputs & forms, cards (talent card, competition card, project card, media card), profile components (avatar, skill chips, portfolio grid), navigation (bottom tab on mobile, top nav on web), tabs, modals/dialogs, alerts, in-app notifications, badges (verified, skill, achievement), tags (skills/categories), leaderboards, score displays (judge scores, weighted results, vote counts).

Principles: single card system with content-type variants; skill/category rendered as colored tag/chip using category-assigned color, not hard-coded per skill; score displays use tabular numerals; leaderboard ranks use gold accent only for top positions.

## 7. Brand ↔ Product Model

The UI must make the multi-talent model legible **without exposing technical terms** `[FRS §3]`:

- Show talent skills as chips/tags on profiles ("Singer · Actor · Director").
- Show a person's capacity contextually: "as Director" within a project, "competing as Singer" within a competition.
- Never render creative skills as permission badges; system roles appear only where operationally relevant (Judge/Admin surfaces).
- A single identity across all contexts — one profile, many skills, many contextual roles.

## 8. Accessibility Baseline

- WCAG AA contrast minimum: 4.5:1 body text, 3:1 large text/UI. *(Validate est. palette: `brand-violet` and `brand-coral` on white likely fail for small text — restrict to large/bold or decorative use until verified.)*
- Focus states visible on all interactive elements (not color-change alone).
- Meaning never carried by color alone — icons/text accompany status colors (submissions, moderation states).
- Touch targets ≥ 44×44px; readable type scale; screen-reader labels on icon-only controls; error states paired with text + icon.

## 9. Experience Consistency

Same brand across Public Web, Mobile, Creator, Judge Portal, Admin Portal — allowed differentiation is *tone density*, not identity: audience/creator = full expressive brand; judge/admin = quieter surfaces, same palette and components.

## 10. Out of Scope (this phase)

Screen design, component implementation, motion spec, icon library, illustration style, copy tone-of-voice guide.
