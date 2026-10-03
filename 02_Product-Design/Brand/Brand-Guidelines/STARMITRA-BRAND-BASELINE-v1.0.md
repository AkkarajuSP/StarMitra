# StarMitra Brand Baseline v1.0

**Product:** StarMitra — *Passion to Perform*
**Status:** Draft — brand/design foundation only
**Scope:** Visual standards derived from the supplied official logo. No screens, no components, no implementation. Does not modify FRS v1.1 or architecture decisions.

## Status Legend

Every statement in this baseline carries one of:

| Status | Meaning |
|--------|---------|
| **OFFICIAL / VERIFIED** | Confirmed against supplied official source artwork |
| **PROVISIONAL** | Derived from the raster logo render — **PENDING SOURCE ARTWORK VERIFICATION** |
| **PROPOSED UI** | Recommendation not established by brand assets — pending design review |
| **MISSING** | Needed but not supplied — request from brand, do not fabricate |

## 1. Brand Identity — OFFICIAL / VERIFIED

| Item | Value | Status |
|------|-------|--------|
| Product | StarMitra | OFFICIAL / VERIFIED (FRS v1.1) |
| Tagline | Passion to Perform | OFFICIAL / VERIFIED (FRS v1.1 + logo lockup) |
| Positioning | Mobile-first talent discovery, creative showcase, competition, collaboration and entertainment-content platform | OFFICIAL / VERIFIED `[FRS §1]` |
| Desired feel | Modern, premium, creative, accessible, energetic — a stage for multi-talented performers | PROPOSED UI |

## 2. Logo

### 2.1 Asset status

Two image renders were supplied and appear visually identical → **treated as the same primary logo** (no separate variants created).

```text
Primary Logo:             AVAILABLE (render; source binary pending drop-in to 02_Product-Design/Brand/Logo/)
Dark/Reversed Logo:       NOT PROVIDED
Emblem/App Mark:          NOT PROVIDED
Tagline-free Lockup:      NOT PROVIDED
Favicon:                  NOT PROVIDED
Source Vector (SVG/EPS):  NOT PROVIDED
```

### 2.2 Composition (observed — PROVISIONAL)

- **Emblem:** navy circular field, white curved swoosh, white-outlined gold five-point star, light-lavender human figure reaching upward, arc of small gold stars — aspiration, performance, achievement.
- **Wordmark:** "Star" violet, "Mitra" near-black ink, bold rounded geometric sans.
- **Tagline:** lowercase "passion to perform," coral, light letter-spaced.

### 2.3 Usage rules — OFFICIAL / VERIFIED (owner directives)

- **Never** modify, recolor, distort, stretch, crop, or redraw the logo. Use the supplied official asset; do not recreate it in CSS/SVG unless explicitly required.
- Preserve logo proportions, original colors, clear space, and aspect ratio.
- The two supplied renders = one primary logo; do not create variants from filenames.
- Missing variants are requested, never fabricated locally.

### 2.4 Usage rules — PROVISIONAL (provisional pending source artwork)

- Clear space ≈ emblem star height on all sides.
- Full lockup minimum width ≈ 120px; emblem-only minimum ≈ 24–28px once that variant exists.
- Dark surfaces: place lockup inside a white/light rounded container OR request a reversed variant — do not recolor locally (the supplied lockup's ink wordmark is designed for light backgrounds).
- Incorrect usage: no recoloring, drop shadows, outlines, rotation, stretching, busy-image placement without container/scrim, re-coloring "Star"/"Mitra" independently, low-contrast placement.

### 2.5 Missing variants — MISSING

Request from brand; do not fabricate:

| Needed variant | Reason |
|----------------|--------|
| Source vector (SVG/EPS/PDF) | Canonical master for all derivatives + color verification |
| Emblem-only mark | App icon, favicon, avatar, compact headers |
| Reversed/light lockup | Navy/dark surfaces, splash screens |
| Tagline-free lockup | Small sizes where tagline is illegible |
| Favicon/app-icon exports | Platform assets |

## 3. Color System

Canonical doc: [`../Colors/STARMITRA-COLOR-SYSTEM.md`](../Colors/STARMITRA-COLOR-SYSTEM.md)

- Brand palette (Navy, Gold, Star Yellow, Violet, Lavender, Ink, Coral, White): **PROVISIONAL — PENDING SOURCE ARTWORK VERIFICATION**
- Surface tint, muted text, border, success/warning/error/info: **PROPOSED UI**

## 4. Typography

Canonical doc: [`../Typography/STARMITRA-TYPOGRAPHY.md`](../Typography/STARMITRA-TYPOGRAPHY.md)

- No official font established by supplied assets.
- **Poppins (headings) + Inter (UI/body): PROPOSED** — must not be classified as official until brand documentation/source assets establish it.

## 5. UI/UX Direction — PROPOSED UI

Mobile-first, modern, premium, creative, accessible. Emotional tone: the "stage" — navy as night sky, gold for achievement, violet for creative energy. Gold and coral are accents, not fills.

## 6. Design System Principles — PROPOSED UI

### 6.1 Layout

- Mobile-first; design at 360px up, adapt to tablet/desktop.
- Spacing scale: 4px base (4, 8, 12, 16, 24, 32, 48).
- One primary action per screen section.
- Touch targets ≥ 44×44px; keyboard-equivalent on web.
- Cards over heavy dividers; generous whitespace.

### 6.2 Component inventory (concept only — do NOT implement)

Buttons (primary navy / secondary violet-outline / destructive), inputs & forms, cards (talent, competition, project, media), profile components (avatar, skill chips, portfolio grid), navigation (mobile bottom tab, web top nav), tabs, modals/dialogs, alerts, in-app notifications, badges (verified, skill, achievement), tags (skills/categories), leaderboards, score displays.

Principles: single card system with content-type variants; skill/category rendered as configurable tag/chip (category-assigned color, not hard-coded per skill); score displays use tabular numerals; gold accent reserved for top leaderboard positions.

## 7. Brand ↔ Product Model — OFFICIAL / VERIFIED (FRS-derived)

UI must make the multi-talent model legible **without exposing technical terms** `[FRS §3]`:

- Talent skills → chips/tags on profiles ("Singer · Actor · Director").
- Contextual capacity → "as Director" in a project, "competing as Singer" in a competition.
- Creative skills are **never** rendered as permission badges; system roles appear only where operationally relevant (Judge/Admin surfaces).
- One identity across all contexts — one profile, many skills, many contextual roles.

## 8. Accessibility Baseline — PROPOSED UI

- WCAG AA: 4.5:1 body text, 3:1 large text/UI. *(Provisional palette: `brand-violet`, `brand-coral`, `brand-gold` on white likely fail small-text contrast — restrict to large/bold/decorative use until verified.)*
- Visible focus states; meaning never carried by color alone; icon+text for status.
- Touch targets ≥ 44×44px; screen-reader labels on icon-only controls; error states = text + icon.

## 9. Experience Consistency — PROPOSED UI

Same brand across Public Web, Mobile, Creator, Judge Portal, Admin Portal — differentiation is tone density, not identity: audience/creator = full expressive brand; judge/admin = quieter surfaces, same palette/components.

## 10. Out of Scope (this phase)

Screen design, component implementation, motion spec, icon library, illustration style, copy tone-of-voice.
