# StarMitra — Color System v1.0

**Parent:** [Brand Baseline](../Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md) | **Status:** Draft — HEX values are **visually estimated from the supplied raster logo** (± tolerance). Verify against the source vector/design file when placed in `../Logo/`.

## 1. Official Brand Palette (extracted)

Colors observed in the official StarMitra logo. Do not replace with generic UI colors.

| Token | Name | HEX (est.) | RGB (est.) | Usage |
|-------|------|-----------|------------|-------|
| `brand-navy` | StarMitra Navy | `#1B2A6B` | 27, 42, 107 | Primary brand color — emblem field, primary surfaces, headers, primary buttons |
| `brand-gold` | Star Gold | `#F6A938` | 246, 169, 56 | Hero accent — main star; highlights, badges, achievement moments |
| `brand-starlight` | Star Yellow | `#F9BE3C` | 249, 190, 60 | Secondary accent — star trail elements; decorative highlights |
| `brand-violet` | StarMitra Violet | `#7F4FE0` | 127, 79, 224 | "Star" wordmark — secondary brand color, links/accents on light surfaces |
| `brand-lavender` | Performer Lavender | `#D8CCF4` | 216, 204, 244 | Performer figure fill — soft accents, decorative fills |
| `brand-ink` | Mitra Ink | `#1B1C2E` | 27, 28, 46 | "Mitra" wordmark — primary text on light backgrounds |
| `brand-coral` | Tagline Coral | `#EE6F77` | 238, 111, 119 | Tagline accent — use sparingly; secondary accent only |
| `brand-white` | White | `#FFFFFF` | 255, 255, 255 | Emblem swoosh/star outline — light surfaces, reversed content on navy |

## 2. Functional Roles (brand-derived)

| Role | Token | HEX (est.) | Notes |
|------|-------|-----------|-------|
| Primary | `brand-navy` | `#1B2A6B` | Dominant brand presence |
| Secondary | `brand-violet` | `#7F4FE0` | Wordmark purple; secondary actions/accents |
| Accent | `brand-gold` | `#F6A938` | Achievements, ratings, premium moments, leaderboards |
| Background (light) | `brand-white` | `#FFFFFF` | Default light surfaces |
| Background (dark) | `brand-navy` | `#1B2A6B` | Dark sections, reversed logo usage |
| Surface | `brand-lavender`-derived tint | `#F4F1FC` *(proposed tint)* | Cards/panels on light background |
| Text (primary) | `brand-ink` | `#1B1C2E` | Body/headings on light |
| Text (muted) | proposed | `#5A5B73` | Secondary text — PROPOSED UI COLOR |
| Border | proposed | `#E2E1EE` | Dividers/borders on light — PROPOSED UI COLOR |

## 3. Functional UI Colors — PROPOSED

Not defined by brand assets. Status: **PROPOSED UI COLOR** — require review; chosen for accessibility (WCAG AA target on their intended backgrounds).

| Role | Token | HEX (proposed) | Usage |
|------|-------|---------------|-------|
| Success | `ui-success` | `#1E8E5A` | Confirmations, qualified/won states |
| Warning | `ui-warning` | `#C77A0A` | Cautions, pending deadlines (darker than brand gold to distinguish semantic vs decorative) |
| Error | `ui-error` | `#D64545` | Errors, rejected/removed states, destructive actions |
| Info | `ui-info` | `#2D7DD2` | Informational banners/notices |

## 4. Dark-Mode Considerations (initial)

`PROPOSED` — dark theme should invert around `brand-navy`/`brand-ink` bases with `brand-gold`/`brand-violet` accents unchanged in hue (adjusted for contrast). Final dark palette requires contrast validation once a UI framework/theme system is chosen.

## 5. Usage Rules

- `brand-gold` and `brand-starlight` are **decorative/celebratory** — not semantic success colors; do not use them for "warning" states.
- `brand-coral` is a **tagline accent** only — not an error/danger color despite being reddish.
- `brand-violet` on white meets AA only at large/bold text sizes *(verify)*; prefer `brand-navy` for body-link contrast.
- Any new color added to the system must record: name, HEX, role, and brand-vs-proposed status.
