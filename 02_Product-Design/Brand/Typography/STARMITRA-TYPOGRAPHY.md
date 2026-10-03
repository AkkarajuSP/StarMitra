# StarMitra — Typography v1.0

**Parent:** [Brand Baseline](../Brand-Guidelines/STARMITRA-BRAND-BASELINE-v1.0.md) | **Status:** PROPOSED — typography is **not officially defined** by the supplied brand assets.

## 1. Findings

The supplied logo is a raster lockup. Two distinct letterform styles appear in the lockup — these describe what is visible, **not** official font identification:

| Element | Observed character |
|---------|-------------------|
| "StarMitra" wordmark | Bold, rounded geometric sans-serif; friendly, contemporary |
| "passion to perform" tagline | Light-weight rounded sans-serif; wide letter-spacing, lowercase |

No font files, specimens, or naming was provided → **no font may be claimed as official.**

## 2. Proposed Type System

`Status: PROPOSED` — Poppins + Inter must **not** be classified as official StarMitra typography until official brand documentation or source assets establish a font. Selected to match the logo's rounded-geometric character, mobile-first readability, and free/open availability.

| Role | Typeface | Fallbacks | Rationale |
|------|----------|-----------|-----------|
| Display / Headings | **Poppins** (SemiBold/Bold) | `"Poppins", "Segoe UI", sans-serif` | Rounded geometric match to wordmark feel; strong multilingual support |
| Body / UI | **Inter** (Regular/Medium/SemiBold) | `"Inter", "Segoe UI", "Roboto", sans-serif` | Best-in-class UI legibility on screens; clear numerals for scores/leaderboards |
| Tagline/eyebrow accents | Poppins Light, letter-spaced | same stack | Echoes the tagline treatment |

Alternatives evaluated: Nunito (softer, closer to tagline feel — acceptable substitute for Poppins); Montserrat (more corporate, less rounded). `PROPOSED` — confirm one heading family in design review.

## 3. Type Scale (mobile-first, PROPOSED)

| Level | Size/weight | Use |
|-------|-------------|-----|
| Display | 32–40 / Poppins Bold | Screen heroes, competition titles |
| H1 | 28 / Poppins SemiBold | Page titles |
| H2 | 22 / Poppins SemiBold | Section headers |
| H3 | 18 / Poppins Medium | Card titles, talent names |
| Body | 16 / Inter Regular | Default text (never below 14 for body) |
| Body Small | 14 / Inter Regular | Metadata, captions |
| Label | 13–14 / Inter Medium | Form labels, buttons |
| Score/numerals | Inter SemiBold, tabular figures | Leaderboards, scores, vote counts |

## 4. Rules

- Minimum body text size: **14px**; interactive labels ≥ 13px.
- Line length: 45–75 characters for reading surfaces.
- Line-height: ≥ 1.4 body, ≥ 1.2 headings.
- Do not use the logo's tagline treatment for running text — wide letter-spacing harms readability below display sizes.
- Verify final font choices render correctly for any target scripts/locales before locking i18n.
