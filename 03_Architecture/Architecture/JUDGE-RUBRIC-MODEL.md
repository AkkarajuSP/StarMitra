# Judge Rubric Model

**Baseline:** StarMitra FRS v1.1 — Multi-Talent / Multi-Skill Model

The judge evaluation system must be **configurable and data-driven**. No criterion, category, scale, or weight may be hard-coded into application logic.

## Admin-Configurable Elements

An administrator must eventually be able to configure:

| Element | Description |
|---------|-------------|
| Category / Skill | The talent category the rubric applies to (references `TalentSkill`) |
| Evaluation Template | A named rubric definition usable across competitions/rounds |
| Criteria | The individual evaluation dimensions within a template |
| Criterion order | Display/entry order of criteria |
| Criterion weight | Relative weight of each criterion in the computed score |
| Scoring scale | Min/max (and step) of the numeric scale per criterion or template |
| Comments | Whether judge comments are mandatory or optional — globally or per criterion |
| Template version | Explicit version number of a template |
| Active / inactive status | Whether a template version is available for new evaluations |

## Immutability Rule

> **A published rubric version is immutable for historical evaluations.**

Once a template version is published and used for evaluations, it must never change. Corrections or improvements require a new version. Historical scores must always be interpretable against the exact rubric definition under which they were recorded.

## Illustrative Examples Only

The following are examples of what an admin might configure — they are NOT fixed schema content:

```text
Singing:
- Voice Quality
- Pitch
- Rhythm
- Expression
- Song Interpretation

Acting:
- Expression
- Dialogue Delivery
- Characterization
- Body Language
- Emotional Impact
```

Criteria sets, weights, and scales are stored as data and administered per category/competition. The implementation must treat all rubric content as configuration data, not code.

## Conceptual Shape (non-normative)

```text
EvaluationTemplate (category/skill, version, status)
   └── RubricCriterion (name, order, weight, scoring scale, comment required?)
```

Evaluation results reference a specific immutable template version, preserving historical integrity.
