---
title: "Postmortem template"
description: "Template for documenting release incidents — what happened, impact, root cause, action items."
---

# Post-Mortem Template

> For use after each major release (v3.0.0 GA and beyond). Replace placeholders in <angle brackets>.

---

# Post-Mortem: worker-kmp v<X.Y.Z>

**Release date:** <YYYY-MM-DD>
**Author:** <person>
**Reviewed by:** <person>

## What shipped

- <comma-separated list of headline features>
- <published artifacts with versions>
- <breaking changes summary>

## What went well

- <thing 1>
- <thing 2>
- <thing 3>

## What didn't

- <issue 1 — what happened + impact>
- <issue 2>
- <issue 3>

## What we learned

- <lesson 1 — actionable for next release>
- <lesson 2>

## Action items

| # | Item | Owner | Due |
|---|------|-------|-----|
| 1 | <action> | <owner> | <date> |
| 2 | | | |

## Metrics

- Alpha → GA wall-clock: <N weeks>
- Alphas cut: <N>
- RCs cut: <N>
- P0 bugs surfaced in burn-in: <N>
- P1 bugs surfaced in burn-in: <N>
- External consumer integration reports: <N>
- HOLISTIC_REVIEW verdict: <Y/N>
- Real-device matrix coverage: <X / Y cells>
- Post-GA hot-fix releases needed: <N> (target: 0)

## Open follow-ups deferred to next release

- <item 1>
- <item 2>
