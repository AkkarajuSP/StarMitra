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

### 2.1 Asset status — OFFICIAL / VERIFIED

Supplied source artwork, stored unmodified in `02_Product-Design/Brand/Logo/`:

| File | Variant | Format | Dimensions | Background |
|------|---------|--------|------------|------------|
| `StarMitra-Primary-Logo.png` | Full lockup — emblem + "StarMitra" wordmark + "passion to perform" tagline | PNG | 1781×1750 | Transparent |
| `StarMitra-Brand-Mark.png` | Emblem/brand mark only | PNG | 1686×1258 | Transparent |

```text
Primary Logo:             AVAILABLE (StarMitra-Primary-Logo.png)
Brand Mark:               AVAILABLE (StarMitra-Brand-Mark.png)
Dark/Reversed Logo:       NOT PROVIDED
White Logo:               NOT PROVIDED
Tagline-free Lockup:      NOT PROVIDED
Monochrome Logo:          NOT PROVIDED
Favicon:                  NOT PROVIDED
App Icon:                 NOT PROVIDED
Source Vector (SVG/EPS):  NOT PROVIDED
```

### 2.2 Intended usage (initial — refinable during UI design)

- **Brand Mark** (`StarMitra-Brand-Mark.png`): app-icon direction, compact navigation, small-space branding, social/profile contexts. *Do not create app icon derivatives yet.*
- **Primary Logo** (`StarMitra-Primary-Logo.png`): website, marketing, login/registration, public landing, official documents, primary brand presence.

### 2.3 Composition (verified — OFFICIAL / VERIFIED)

- **Emblem:** navy `#022179` circular field, lavender `#E9E5FF` curved swoosh (renders as "white"), gold `#F5B942` five-point star with lavender outline, lavender performer figure reaching upward with deep-violet `#5B3FD3` shading, arc of small gold stars — aspiration, performance, achievement.
- **Wordmark:** "Star" purple `#7C3AED`, "Mitra" ink `#101828`, bold rounded geometric sans.
- **Tagline:** lowercase "passion to perform," coral `#F05A5E`, light letter-spaced.
- **Background:** transparent in both PNGs — artwork sits cleanly on light surfaces; dark surfaces need a light container or a (not-yet-provided) reversed variant.

### 2.3 Usage rules — OFFICIAL / VERIFIED (owner directives)

- **Never** modify, recolor, distort, stretch, crop, or redraw the logo. Use the supplied official asset; do not recreate it in CSS/SVG unless explicitly required.
- Preserve logo proportions, original colors, clear space, and aspect ratio.
- The two supplied renders = one primary logo; do not create variants from filenames.
- Missing variants are requested, never fabricated locally.

### 2.4 Usage rules — PROVISIONAL

- Clear space ≈ emblem star height on all sides.
- Full lockup minimum width ≈ 120px; brand mark minimum ≈ 24–28px.
- Dark surfaces: transparent PNGs sit on light backgrounds; for navy/dark surfaces place in a light rounded container OR request a reversed variant — do not recolor locally.
- Incorrect usage: no recoloring, drop shadows, outlines, rotation, stretching, busy-image placement without container/scrim, re-coloring "Star"/"Mitra" independently, low-contrast placement.
- Do not recreate the logo in HTML/CSS/SVG — the supplied PNGs are the brand source; if SVG/vector is supplied later it becomes the preferred production asset.

### 2.5 Missing variants — MISSING

Request from brand; do not fabricate:

| Needed variant | Reason |
|----------------|--------|
| Source vector (SVG/EPS/PDF) | Canonical master + preferred production asset |
| Reversed/light lockup | Navy/dark surfaces, splash screens |
| Monochrome variant | Single-color print/contexts |
| Tagline-free lockup | Small widths where tagline is illegible |
| Favicon/app-icon exports | Platform assets (derive from Brand Mark when approved) |

## 3. Color System

Canonical doc: [`../Colors/STARMITRA-COLOR-SYSTEM.md`](../Colors/STARMITRA-COLOR-SYSTEM.md)

- Primary palette — **OFFICIAL / VERIFIED** via pixel analysis: Navy `#022179`, Ink `#101828`, Lavender `#E9E5FF`, Purple `#7C3AED`, Gold `#F5B942`, Coral `#F05A5E`; secondary Deep Violet `#5B3FD3`.
- `ui-white #FFFFFF` (artwork uses lavender/transparent, not pure white), surface tint, muted text, border, success/warning/error/info: **PROPOSED UI**

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

- WCAG AA: 4.5:1 body text, 3:1 large text/UI. *(`brand-coral` and `brand-gold` on white fail AA for small text — decorative/large-text only; `brand-purple` ~5.9:1 borderline — verify before small-text use.)*
- Visible focus states; meaning never carried by color alone; icon+text for status.
- Touch targets ≥ 44×44px; screen-reader labels on icon-only controls; error states = text + icon.

## 9. Experience Consistency — PROPOSED UI

Same brand across Public Web, Mobile, Creator, Judge Portal, Admin Portal — differentiation is tone density, not identity: audience/creator = full expressive brand; judge/admin = quieter surfaces, same palette/components.

## 10. Out of Scope (this phase)

Screen design, component implementation, motion spec, icon library, illustration style, copy tone-of-voice.
