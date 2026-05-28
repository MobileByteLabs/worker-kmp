#!/usr/bin/env bash
# scripts/check-ios-artifact.sh — RED → GREEN gate for cmp-worker-compose iOS publication.
#
# Authored 2026-05-28 as the RED commit of:
#   plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/
#     01-compose-ios-target.md — Task T2
#
# Asserts that cmp-worker-compose has a BCV klib snapshot at the canonical path,
# and that the snapshot's `// Targets:` header includes both iosArm64 and
# iosSimulatorArm64 (the inferred-merged BCV 0.17 klib API dump covers all
# native + JS targets in a single file).
#
# Exits 1 until Phase 1 GREEN lands (iosArm64() + iosSimulatorArm64() targets in
# cmp-worker-compose/build.gradle.kts + apiValidation.klib.enabled = true in root
# build.gradle.kts + ./gradlew :cmp-worker-compose:apiDump committed).
#
# Cross-reference: GOAL.md AC1 + AC2 + AC9 (partial).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

snapshot="cmp-worker-compose/api/cmp-worker-compose.klib.api"

if [[ ! -f "$snapshot" ]]; then
    echo "❌ FAIL: cmp-worker-compose klib BCV snapshot missing." >&2
    echo "    - $snapshot" >&2
    echo "" >&2
    echo "Resolution:" >&2
    echo "  1. Confirm iosArm64() + iosSimulatorArm64() targets are in" >&2
    echo "     cmp-worker-compose/build.gradle.kts." >&2
    echo "  2. Confirm apiValidation { klib { enabled = true } } is set in" >&2
    echo "     the root build.gradle.kts." >&2
    echo "  3. Run ./gradlew :cmp-worker-compose:apiDump." >&2
    exit 1
fi

# Confirm the snapshot enumerates the iOS targets BCV is tracking.
required_targets=("iosArm64" "iosSimulatorArm64")
target_line="$(grep -m1 '^// Targets:' "$snapshot" || true)"

if [[ -z "$target_line" ]]; then
    echo "❌ FAIL: $snapshot is missing the '// Targets: [...]' header." >&2
    echo "    The file exists but does not look like a BCV klib dump." >&2
    exit 1
fi

for target in "${required_targets[@]}"; do
    if ! grep -qE "\\b${target}\\b" <<< "$target_line"; then
        echo "❌ FAIL: $snapshot does not list target '$target'." >&2
        echo "    Header line: $target_line" >&2
        echo "    Re-run ./gradlew :cmp-worker-compose:apiDump after adding the target." >&2
        exit 1
    fi
done

echo "✅ cmp-worker-compose klib BCV snapshot present, targets: $(sed 's|^// Targets: ||' <<< "$target_line")"
