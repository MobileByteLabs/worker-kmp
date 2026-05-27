# Release Process

> Pre-GA stabilization for worker-kmp v3.0.0. Replaces the alpha → GA jump with a
> deliberate beta + 2-RC cycle. Per Phase 14 of the v3.0.0 epic.

## Ship plan

| Stage | Purpose | Burn-in | Gate to advance |
|-------|---------|---------|-----------------|
| v3.0.0-beta01 | First "try this" — consolidates alpha00-alpha08 + bug fixes | 7 days | No P0 + CI all GREEN on development for 7 consecutive days + perf bench within 10% of latest stable |
| v3.0.0-rc1 | beta01 + bug fixes + holistic review | 14 days | ≥3 external consumer projects integrate + no P0 + ≤2 P1 + HOLISTIC_REVIEW.md signed off |
| v3.0.0-rc2 | rc1 + fixes (only if needed) | 7 days | Zero P0/P1 burn-in |
| v3.0.0 GA | RC + version bump only — no code changes | n/a | All G-N + release notes ready + 30-day post-GA hot-fix window starts |

## Cutting a beta / RC

1. `release/v3.0.0-beta01/` branch off `feat/v3-epic-foreground-storeflow`
2. Consolidate alpha CHANGELOG entries into single `[3.0.0-beta01]` section
3. `./gradlew apiCheck` — capture public API snapshot as baseline
4. Tag + publish all 7 artifacts as `3.0.0-beta01`
5. GitHub Release + X/Mastodon post + KMP Slack announce
6. Setup `v3-beta-feedback` GitHub Issues label
7. Daily monitoring during burn-in (CI, Issues, Maven Central download stats)

## Holistic review checklist (for rc1)

A fresh reviewer (not the implementer) signs off on each axis:

- **Security** — re-run STRIDE per SECURITY.md against actual ship code (not design)
- **Performance** — re-run benchmarks on clean machine; compare against published baselines
- **Backward compatibility** — pull worker-kmp:2.1.0 as classpath dep + run real Mifos Money Toolkit fork against worker-kmp:3.0.0-rc1; report any surprises
- **API ergonomics** — read public API via BCV snapshots end-to-end; list naming/shape/discoverability concerns
- **Documentation** — read every doc (SECURITY, PERFORMANCE, OBSERVERS, BACKWARD_COMPATIBILITY, MIGRATION, FOREGROUND_TASKS, PLATFORM_API_MATRIX, TRUE_BACKGROUND_MATRIX); flag inconsistency/missing/stale
- **Accessibility** — Compose components have Modifier.semantics + content descriptions + ≥4.5:1 color contrast
- **Internationalization** — strings extracted to commonMain resource files (not hardcoded English)

Output: `release/v3.0.0-rc1/HOLISTIC_REVIEW.md` with explicit GA-ready: [Y/N] verdict.

## External consumer integration program (rc1 — 14-day window)

Reach out to 3+ KMP projects (including framework's own: Mifos Money Toolkit, healld-health, expense-tracker, plus 2 external) to integrate RC1 + capture experience reports.

Output: `release/v3.0.0-rc1/CONSUMER_REPORTS.md` with per-consumer:
- Integration wall-clock time (target ≤30 min for small app)
- Issues encountered + how migration toolkit + docs handled them
- Verdict: Ship as-is / Block on issue X / Document caveat Y

## Real-device verification matrix (rc1)

| Platform | Devices | Phases verified |
|----------|---------|-----------------|
| Android | Galaxy S10 (API 30) + Pixel 7 (API 34) + Galaxy J3 (API 21) | Phase 1 foreground + Phase 7 expedited + Phase 7 service types |
| iOS | iPhone 8 (iOS 16) + iPhone 14 (iOS 17+) + iPad | Phase 1 + Phase 7 BGAppRefresh + Phase 7 BGContinuedProcessing |
| Desktop | Win11 + macOS 14 Sonoma + Ubuntu 22.04 + Fedora 39 + Debian 12 | Phase 8 daemon install / post-close / post-reboot |
| Web | Chrome 120 + Firefox 121 + Safari 17 (macOS) + Safari 17 (iOS PWA) + Edge 120 | Phase 9 Web Push roundtrip |

## Post-mortem (within 14 days of GA)

`archive/<YYYY-MM>/worker-kmp-v3-foreground-storeflow/POSTMORTEM.md` per `POSTMORTEM_TEMPLATE.md` below.
