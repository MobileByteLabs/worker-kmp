#!/usr/bin/env bash
# bc-test-diff.sh — runs v2BcTest + v3BcTest, diffs the JUnit XML outputs,
# fails CI when test results diverge between v2.1.0 and current v3 classpaths.
#
# Why: ensures deprecated v2 init wrappers (added in Phase 0) keep working through
# the v3.x grace window. Catches silent rot.

set -euo pipefail

echo "Running v2 BC test (against worker-kmp:2.1.0)..."
./gradlew :cmp-worker-bc-test:v2BcTest --rerun-tasks --no-daemon

echo ""
echo "Running v3 BC test (against current local build)..."
./gradlew :cmp-worker-bc-test:v3BcTest --rerun-tasks --no-daemon

V2_XML="cmp-worker-bc-test/build/reports/junit/v2-bc-test"
V3_XML="cmp-worker-bc-test/build/reports/junit/v3-bc-test"

echo ""
echo "Diffing test results..."

# Normalize the JUnit XML before diffing (strip timing fields that vary per run)
normalize() {
    sed -E 's/time="[^"]+"//g; s/timestamp="[^"]+"//g; s/hostname="[^"]+"//g' "$1"
}

for v2_file in "${V2_XML}"/TEST-*.xml; do
    base=$(basename "${v2_file}")
    v3_file="${V3_XML}/${base}"
    if [ ! -f "${v3_file}" ]; then
        echo "✗ v3 missing report file: ${base}"
        exit 1
    fi
    if ! diff -q <(normalize "${v2_file}") <(normalize "${v3_file}") > /dev/null; then
        echo "✗ BC divergence in ${base}:"
        diff <(normalize "${v2_file}") <(normalize "${v3_file}") | head -40
        exit 1
    fi
done

echo "✓ All BC tests produced identical results across v2 and v3 classpaths."
exit 0
