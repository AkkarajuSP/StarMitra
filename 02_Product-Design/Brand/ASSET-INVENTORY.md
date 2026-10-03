# StarMitra — Brand Asset Inventory

**Parent:** [Brand Baseline](Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md) | **Status:** Updated 2026-10-03

## Supplied assets — registered

| Filename | Format | Dimensions | Variant | Background | Intended Usage | Source | Verification Status |
|----------|--------|------------|---------|------------|----------------|--------|---------------------|
| `StarMitra-Primary-Logo.png` | PNG | 1781×1750 | Primary full lockup (emblem + wordmark + tagline) | Transparent | Website, marketing, login/registration, public landing, official docs, primary brand presence | Owner-supplied (`SMlogoMain.png`) | **OFFICIAL / VERIFIED** — pixel-analyzed; palette extracted |
| `StarMitra-Brand-Mark.png` | PNG | 1686×1258 | Brand mark / emblem only | Transparent | App-icon direction, compact nav, small-space branding, social/profile | Owner-supplied (`Brand.png`) | **OFFICIAL / VERIFIED** — pixel-analyzed; palette extracted |

Both files stored unmodified in `Logo/` (originals remain at their source location).

## Variant availability

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

## Observed but NOT registered

| File | Location | Observation | Status |
|------|----------|-------------|--------|
| `SMLogo.png` (1797×1681) | `OneDrive\Desktop\New folder\` | Alternate lockup: pink `#EC4899` tagline and simplified emblem without star trail | **NOT REGISTERED** — not supplied as a brand asset; register on owner confirmation |

## Requested / needed assets — MISSING

| Asset | Type | Purpose | Status |
|-------|------|---------|--------|
| Source vector logo | SVG/EPS/PDF | Canonical master; preferred production asset | Requested |
| Reversed/light lockup | SVG/PNG | Dark/navy surfaces | Requested |
| Monochrome variant | SVG/PNG | Single-color contexts | Requested |
| Tagline-free lockup | SVG/PNG | Small widths where tagline illegible | Requested |
| Favicon/app-icon exports | PNG/ICO | Platform assets — derive from Brand Mark when approved | Requested |
| UI icon set | SVG | Product iconography | Not supplied — direction TBD in design phase |

## Rules

- One canonical file per variant — no duplicates; originals preserved unmodified; vector preferred when available.
- Official assets live only under `02_Product-Design/Brand/`; frontends export from a single source once a build pipeline exists.
- Raster assets never upscaled beyond source resolution.
- Do not recreate/redraw the logo (HTML/CSS/SVG) — supplied artwork is the brand source.
