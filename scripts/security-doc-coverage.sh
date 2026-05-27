#!/usr/bin/env bash
# security-doc-coverage.sh — fails CI when new files under attack-surface directories
# are added without a corresponding docs/operations/security.md row update in the same PR.
#
# Usage:
#   bash scripts/security-doc-coverage.sh <base-ref> <head-ref>
#
# Defaults: base=origin/development, head=HEAD
#
# Exit codes:
#   0  OK — either no surface change, or docs/operations/security.md was also touched in the diff range
#   1  Surface added without docs/operations/security.md update — CI should fail with a clear pointer

set -euo pipefail

BASE_REF="${1:-origin/development}"
HEAD_REF="${2:-HEAD}"

# Directories that introduce attack surfaces (per Phase 10 docs/operations/security.md scope)
SURFACE_DIRS=(
    "cmp-worker-desktop-daemon/"
    "cmp-worker-web-push/"
    "cmp-worker-koin/"      # Koin factory injection (T22-T24)
    "cmp-worker-kmp/"       # ForegroundWorker notification body (T26)
)

CHANGED_FILES=$(git diff --name-only "${BASE_REF}...${HEAD_REF}" 2>/dev/null || true)
if [ -z "${CHANGED_FILES}" ]; then
    echo "✓ No changes in diff range ${BASE_REF}...${HEAD_REF}"
    exit 0
fi

SURFACE_TOUCHED=0
for dir in "${SURFACE_DIRS[@]}"; do
    if echo "${CHANGED_FILES}" | grep -q "^${dir}"; then
        SURFACE_TOUCHED=1
        echo "  surface touched: ${dir}"
    fi
done

if [ "${SURFACE_TOUCHED}" -eq 0 ]; then
    echo "✓ No attack-surface directories touched"
    exit 0
fi

# Surface was touched — docs/operations/security.md must also be in the diff
if echo "${CHANGED_FILES}" | grep -q "^docs/operations/security\.md$"; then
    echo "✓ Attack surface touched AND docs/operations/security.md updated in same PR"
    exit 0
fi

echo ""
echo "✗ Attack surface touched but docs/operations/security.md NOT updated."
echo ""
echo "  Per RULE-SECRETS-VAULT-001 + Phase 10 of the worker-kmp v3.0.0 epic,"
echo "  any change to an attack-surface directory MUST be accompanied by a"
echo "  corresponding update to docs/operations/security.md's STRIDE matrix (new row OR row update)."
echo ""
echo "  Either:"
echo "    1. Add/update a docs/operations/security.md row reflecting the new threat profile, OR"
echo "    2. Confirm the change has no security impact and check the 'N/A' box"
echo "       in the PR template's ## Security review section."
echo ""
exit 1
