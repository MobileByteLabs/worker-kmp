#!/usr/bin/env bash
# perf-capture-baseline.sh — capture JMH baseline JSON for a release.
#
# Usage: bash scripts/perf-capture-baseline.sh <version>
# Output: perf-baselines/<version>.json
#
# Phase 11 (PLAN worker-kmp-v3-foreground-storeflow-11-performance-benchmarks).

set -euo pipefail
VERSION="${1:?usage: perf-capture-baseline.sh <version>}"

mkdir -p perf-baselines
./gradlew :cmp-worker-bench:jmh -Pjmh.resultFormat=JSON -Pjmh.resultsFile=perf-baselines/${VERSION}.json
echo "✓ Captured baseline to perf-baselines/${VERSION}.json"
