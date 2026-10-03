# StarMitra — Brand Asset Inventory

**Parent:** [Brand Baseline](Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md) | **Status:** Draft

## Supplied assets

| Asset | Type | Purpose | Recommended Usage | Location |
|-------|------|---------|-------------------|----------|
| StarMitra full lockup — light background (emblem + "StarMitra" wordmark + "passion to perform" tagline) | Raster image (received in-conversation) | Primary brand identity | Splash/landing headers, marketing surfaces, about screens, light-background branding | **PENDING** — source file to be placed in `Logo/` |
| Second render of same lockup | Raster image | Appears identical to above — treated as duplicate unless a distinction is provided | — | **PENDING** |

> Two renders were supplied; they appear identical. If they are intended as distinct variants (e.g., different resolutions or a subtle variant), clarify and this table will be updated. No duplicate file will be stored.

## Expected / needed assets (not yet supplied)

| Asset | Type | Purpose | Recommended Usage | Status |
|-------|------|---------|-------------------|--------|
| Source vector logo | SVG/EPS/PDF | Canonical master | All derivatives | Requested |
| Emblem-only mark | SVG/PNG | App icon, favicon, avatars, compact headers | Small formats | Requested |
| Reversed/light lockup | SVG/PNG | Dark/navy surfaces | Dark theme, splash | Requested |
| Tagline-free lockup | SVG/PNG | Small widths where tagline illegible | Compact branding | Requested |
| App icon / favicon exports | PNG/ICO | Platform assets | Stores, browser | Requested |
| Icon set | SVG | UI iconography | `Icons/` | Not supplied — system TBD in design phase |

## Rules

- One canonical file per variant — no unnecessary duplicates.
- Store official assets only in `02_Product-Design/Brand/` — never duplicated into `07_Development/`; frontends reference/export from a single source when a build pipeline exists.
- Raster exports must never be upscaled beyond source resolution.
