# StarMitra — Color System v1.0

**Parent:** [Brand Baseline](../Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md)

## Status Legend

| Status | Meaning |
|--------|---------|
| **OFFICIAL / VERIFIED** | Pixel-verified against supplied source artwork (`StarMitra-Brand-Mark.png`, `StarMitra-Primary-Logo.png`) |
| **PROVISIONAL** | Estimated — pending verification |
| **PROPOSED UI COLOR** | Not in brand assets — proposed functional color pending design review |

**Verification method:** full-pixel scan of supplied PNGs; dominant colors counted, anti-aliasing/shading blends excluded from primary status.

## 1. Official Brand Palette — OFFICIAL / VERIFIED

Verified by direct pixel analysis (2026-10-03). Coverage shown is share of visible (non-transparent) pixels in the primary lockup.

| Token | Name | HEX | RGB | Source | Usage | Status |
|-------|------|-----|-----|--------|-------|--------|
| `brand-navy` | Primary Navy | `#022179` | 2, 33, 121 | Artwork ~66% coverage | Emblem field; primary brand surfaces, headers | OFFICIAL / VERIFIED |
| `brand-ink` | Primary Ink | `#101828` | 16, 24, 40 | "Mitra" wordmark ~10% | Primary text on light; wordmark | OFFICIAL / VERIFIED |
| `brand-lavender` | Primary Lavender | `#E9E5FF` | 233, 229, 255 | Figure + swoosh + star outline ~9% | Performer figure; light accents; **functions as "white" inside artwork** | OFFICIAL / VERIFIED |
| `brand-purple` | Primary Purple | `#7C3AED` | 124, 58, 237 | "Star" wordmark ~7% | Secondary brand accent; links/highlights on light | OFFICIAL / VERIFIED |
| `brand-gold` | Primary Gold | `#F5B942` | 245, 185, 66 | Stars ~5% | Achievement/star accent; badges, ratings, leaderboards | OFFICIAL / VERIFIED |
| `brand-coral` | Primary Coral | `#F05A5E` | 240, 90, 94 | Tagline ~1% | Tagline accent only — sparing decorative use | OFFICIAL / VERIFIED |
| `brand-violet-deep` | Deep Violet | `#5B3FD3` | 91, 63, 211 | Emblem figure shading ~1% | Emblem-internal shading — use only as part of artwork | OFFICIAL / VERIFIED (secondary; not a UI color) |

## 2. White — Clarification

Pure `#FFFFFF` is **not present in the artwork** (0.0% of pixels): what renders as "white" is `brand-lavender #E9E5FF` (swoosh, star outline, figure) or **transparency** — both PNGs ship with transparent backgrounds (~49–58% transparent pixels).

| Token | HEX | Status |
|-------|-----|--------|
| `ui-white` | `#FFFFFF` | PROPOSED UI COLOR — light surfaces/backgrounds, not an artwork color |

## 3. Anti-aliasing / Edge Colors (excluded from palette)

Not brand colors — do not promote:

| HEX | Nature |
|-----|--------|
| `#011347` | Dark navy edge/shading variant of `brand-navy` (~0.2%) |
| `#DBD9F7`, `#B0B5DE`, `#7683BC`, `#3C529B`, `#596BAB` | Lavender/navy anti-aliasing blends |
| `#E7B045`, `#B99350` | Gold anti-aliasing/shadow blends |
| `#3F476B`, `#112B76`, `#102D81` | Navy transition blends |

## 4. Functional Roles

| Role | Token | HEX | Status |
|------|-------|-----|--------|
| Primary | `brand-navy` | `#022179` | OFFICIAL / VERIFIED |
| Secondary | `brand-purple` | `#7C3AED` | OFFICIAL / VERIFIED |
| Accent | `brand-gold` | `#F5B942` | OFFICIAL / VERIFIED |
| Background (light) | `ui-white` | `#FFFFFF` | PROPOSED UI COLOR |
| Background (dark) | `brand-navy` | `#022179` | OFFICIAL / VERIFIED |
| Surface (light) | lavender tint | `#F4F2FD` | PROPOSED UI COLOR |
| Text (primary) | `brand-ink` | `#101828` | OFFICIAL / VERIFIED |
| Text (muted) | `ui-text-muted` | `#5A5B73` | PROPOSED UI COLOR |
| Border | `ui-border` | `#E2E1EE` | PROPOSED UI COLOR |

## 5. Functional UI Colors — PROPOSED UI COLOR

| Role | Token | HEX | Usage |
|------|-------|-----|-------|
| Success | `ui-success` | `#1E8E5A` | Confirmations, qualified/won states |
| Warning | `ui-warning` | `#C77A0A` | Cautions, pending deadlines |
| Error | `ui-error` | `#D64545` | Errors, rejected states, destructive actions |
| Info | `ui-info` | `#2D7DD2` | Informational banners/notices |

## 6. Usage Rules

- `brand-gold` = decorative/celebratory only — not semantic success.
- `brand-coral` = tagline accent only — not an error color despite being reddish.
- `brand-lavender` plays the "white" role inside artwork; in UI, surfaces use true `ui-white` or lavender tints — keep them visually distinct.
- Contrast note: `brand-purple` on white ≈ 5.9:1 *(verify)* — borderline for small text; `brand-coral` and `brand-gold` on white fail AA for small text — decorative/large-text use only.
- If vector artwork is supplied later, re-verify all HEX values and promote/demote statuses accordingly.
