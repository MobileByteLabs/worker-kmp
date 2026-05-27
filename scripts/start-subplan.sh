#!/usr/bin/env bash
# start-subplan.sh — One-shot worktree creator for plan-driven sub-plans.
#
# Usage: ./scripts/start-subplan.sh <sub-plan-slug>
#
# Opens an isolated git worktree off the source repo's default branch at
# ../worker-kmp-{slug}/ on branch feat/{slug}, then prints the next steps.
#
# Part of the claude-product-cycle framework's planning-rigor scaffold
# (Phase 6 of the worker-kmp v3.0.0 epic).
#
# Conventions:
#   - Slug must match a sub-plan slug at
#     plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/
#     (e.g. worker-kmp-v3-foreground-storeflow-01-foreground-tasks)
#   - Worktree path is sibling to this repo: ../worker-kmp-{slug}
#   - Branch name is feat/{slug}
#
# Exits 1 if:
#   - No slug argument
#   - Worktree path already exists
#   - Branch feat/{slug} already exists
#   - Not run from the source repo root

set -euo pipefail

SLUG="${1:?usage: start-subplan.sh <sub-plan-slug>}"

# Sanity: must be in the source repo root (where this script lives)
if [ ! -f "settings.gradle.kts" ] || [ ! -d "scripts" ]; then
    echo "✗ Run from the source repo root (where settings.gradle.kts lives)." >&2
    exit 1
fi

WORKTREE_DIR="../worker-kmp-${SLUG}"
BRANCH="feat/${SLUG}"

# Refuse if worktree dir already exists
if [ -e "${WORKTREE_DIR}" ]; then
    echo "✗ Worktree path already exists: ${WORKTREE_DIR}" >&2
    echo "  Remove it (git worktree remove ${WORKTREE_DIR}) or pick a different slug." >&2
    exit 1
fi

# Refuse if branch already exists locally
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    echo "✗ Branch already exists locally: ${BRANCH}" >&2
    echo "  Delete it (git branch -D ${BRANCH}) or pick a different slug." >&2
    exit 1
fi

git worktree add "${WORKTREE_DIR}" -b "${BRANCH}"

echo ""
echo "✓ Worktree created at ${WORKTREE_DIR}"
echo "✓ Branch ${BRANCH} checked out in worktree"
echo ""
echo "Next steps:"
echo "  cd ${WORKTREE_DIR}"
echo "  # Open the matching sub-plan file from the framework root:"
echo "  #   plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/${SLUG##worker-kmp-v3-foreground-storeflow-}.md"
echo "  # Execute tasks in order, RED-first per task."
echo "  # Open PR via .github/PULL_REQUEST_TEMPLATE.md when all tasks complete."
