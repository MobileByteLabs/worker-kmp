#!/usr/bin/env bash
# perf-regression-check.sh — fail CI if any JMH metric regresses >20% vs latest baseline.
#
# Usage: bash scripts/perf-regression-check.sh
#
# Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).

set -euo pipefail

# Find latest baseline (lexicographically — versions sort right when zero-padded; use -V for SemVer)
LATEST_BASELINE=$(ls perf-baselines/*.json 2>/dev/null | grep -v README | sort -V | tail -1 || true)
if [ -z "${LATEST_BASELINE}" ]; then
    echo "✓ No baseline exists yet — skipping regression check (first PR pre-v2.2.0)."
    exit 0
fi

CURRENT_RESULTS="build/results/jmh/results.json"

echo "Running JMH (quick mode — fewer iterations)..."
./gradlew :cmp-worker-bench:jmh -Pjmh.iterations=2 -Pjmh.warmupIterations=2 -Pjmh.fork=1 \
    -Pjmh.resultFormat=JSON -Pjmh.resultsFile="${CURRENT_RESULTS}" --no-daemon

if [ ! -f "${CURRENT_RESULTS}" ]; then
    echo "✗ JMH did not produce results file."
    exit 1
fi

echo ""
echo "Comparing ${CURRENT_RESULTS} against baseline ${LATEST_BASELINE}..."

# Per-benchmark comparison via jq (assumes both files are JMH JSON arrays)
REGRESSIONS=$(jq -r --slurpfile baseline "${LATEST_BASELINE}" '
    . as $cur
    | $baseline[0] as $base
    | [
        $cur[] as $c
        | $base[] | select(.benchmark == $c.benchmark)
        | { benchmark: .benchmark,
            base: .primaryMetric.score,
            cur: $c.primaryMetric.score,
            ratio: ($c.primaryMetric.score / .primaryMetric.score) }
        | select(.ratio > 1.20)
        | "  ✗ \(.benchmark): \(.base | tostring) → \(.cur | tostring) (ratio \(.ratio | tostring))"
    ]
    | .[]
' "${CURRENT_RESULTS}")

if [ -n "${REGRESSIONS}" ]; then
    echo "REGRESSION DETECTED (>20% slowdown):"
    echo "${REGRESSIONS}"
    exit 1
fi

echo "✓ No regression beyond 20% threshold."
exit 0
