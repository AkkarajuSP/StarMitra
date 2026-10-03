# StarMitra — Color System v1.0

**Parent:** [Brand Baseline](../Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md)

## Status Legend

| Status | Meaning |
|--------|---------|
| **PROVISIONAL** | Visually estimated from the supplied raster logo render — **PENDING SOURCE ARTWORK VERIFICATION**. Not official until verified against the original vector/source file. |
| **PROPOSED UI COLOR** | Not present in brand assets — proposed functional/UI color pending design review. |
| **OFFICIAL / VERIFIED** | Confirmed against official source artwork. *(Nothing currently holds this status.)* |

> **All brand HEX values below are `STATUS: PROVISIONAL — PENDING SOURCE ARTWORK VERIFICATION`.**
> Do not treat them as official, lock them into code tokens, or publish them externally until verified against the logo source file.

## 1. Brand Palette — PROVISIONAL

Colors observed in the supplied StarMitra logo render.

| Token | Name | HEX (PROVISIONAL) | RGB (est.) | Usage |
|-------|------|-------------------|------------|-------|
| `brand-navy` | StarMitra Navy | `#1B2A6B` | 27, 42, 107 | Primary brand presence — emblem field, primary surfaces, headers |
| `brand-gold` | Star Gold | `#F6A938` | 246, 169, 56 | Hero accent — main star; achievements, badges |
| `brand-starlight` | Star Yellow | `#F9BE3C` | 249, 190, 60 | Secondary accent — star trail; decorative highlights |
| `brand-violet` | StarMitra Violet | `#7F4FE0` | 127, 79, 224 | "Star" wordmark — secondary brand accent |
| `brand-lavender` | Performer Lavender | `#D8CCF4` | 216, 204, 244 | Performer figure fill — soft decorative accents |
| `brand-ink` | Mitra Ink | `#1B1C2E` | 27, 28, 46 | "Mitra" wordmark — primary text on light |
| `brand-coral` | Tagline Coral | `#EE6F77` | 238, 111, 119 | Tagline accent — sparing secondary accent |
| `brand-white` | White | `#FFFFFF` | 255, 255, 255 | Emblem swoosh/star outline — light surfaces |

**Verification method when source arrives:** sample HEX directly from the vector/source artwork for each element above; replace values and flip status to OFFICIAL / VERIFIED.

## 2. Functional Roles (brand-derived — PROVISIONAL)

| Role | Token | HEX | Notes |
|------|-------|-----|-------|
| Primary | `brand-navy` | `#1B2A6B` PROVISIONAL | Dominant brand presence |
| Secondary | `brand-violet` | `#7F4FE0` PROVISIONAL | Secondary actions/accents |
| Accent | `brand-gold` | `#F6A938` PROVISIONAL | Achievements, ratings, leaderboards |
| Background (light) | `brand-white` | `#FFFFFF` PROVISIONAL | Default light surfaces |
| Background (dark) | `brand-navy` | `#1B2A6B` PROVISIONAL | Dark sections / reversed logo surfaces |
| Surface | tint of `brand-lavender` | `#F4F1FC` PROPOSED UI COLOR | Cards/panels on light background |
| Text (primary) | `brand-ink` | `#1B1C2E` PROVISIONAL | Body/headings on light |
| Text (muted) | `ui-text-muted` | `#5A5B73` PROPOSED UI COLOR | Secondary text |
| Border | `ui-border` | `#E2E1EE` PROPOSED UI COLOR | Dividers/borders on light |

## 3. Functional UI Colors — PROPOSED UI COLOR

Not defined by the brand assets; chosen with WCAG AA contrast intent. Pending design review.

| Role | Token | HEX | Usage |
|------|-------|-----|-------|
| Success | `ui-success` | `#1E8E5A` PROPOSED UI COLOR | Confirmations, qualified/won states |
| Warning | `ui-warning` | `#C77A0A` PROPOSED UI COLOR | Cautions, pending deadlines |
| Error | `ui-error` | `#D64545` PROPOSED UI COLOR | Errors, rejected states, destructive actions |
| Info | `ui-info` | `#2D7DD2` PROPOSED UI COLOR | Informational banners/notices |

## 4. Dark-Mode Direction — PROPOSED UI

Dark theme inverts around `brand-navy`/`brand-ink` bases with `brand-gold`/`brand-violet` accents (contrast-adjusted). Requires validation once the verified palette lands.

## 5. Usage Rules

- `brand-gold` / `brand-starlight` are **decorative/celebratory** — not semantic success/warning.
- `brand-coral` is a **tagline accent** only — not an error/danger color despite being reddish.
- Until values are verified, any UI implementation must treat the palette as subject to change.
- Any added color records: name, HEX, role, and status (PROVISIONAL / PROPOSED UI / OFFICIAL-VERIFIED).
