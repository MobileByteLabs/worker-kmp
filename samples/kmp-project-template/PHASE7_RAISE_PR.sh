#!/usr/bin/env bash
# Phase 7 — raise upstream PR against openMF/kmp-project-template
#
# RUN THIS ONLY AFTER:
#   1. ./gradlew :build-logic:convention:assemble                    # Phase 2 T5
#   2. ./gradlew :core:data:commonTest                                # Phase 3 T8
#   3. ./gradlew :sync:commonTest :sync:assemble                      # Phase 4 T11/T12
#   4. ./gradlew :samples:kmp-project-template:cmp-android:assembleDebug  # Phase 5 T5
#   5. ./gradlew :samples:kmp-project-template:assemble               # Phase 5 T6
#   6. Manual Android emulator smoke test (AC-12)                     # Phase 5 T7
#   7. ./gradlew spotlessApply detekt && bash scripts/pre-commit.sh   # Phase 6 T7
#   8. You have explicit go-ahead to publish a PR to openMF/kmp-project-template
#
# This script is IDEMPOTENT-ISH: re-running creates a 2nd PR if a 1st is already open
# under the same branch name. Check `gh pr list --repo openMF/kmp-project-template
# --head therajanmaurya:feat/worker-kmp-sync-module` before re-running.

set -euo pipefail

UPSTREAM="openMF/kmp-project-template"
UPSTREAM_DEFAULT_BRANCH="dev"
FORK_OWNER="${GH_FORK_OWNER:-therajanmaurya}"
FEAT_BRANCH="feat/worker-kmp-sync-module"
WORK_DIR="${TMPDIR:-/tmp}/openmf-kmp-project-template-fork-$$"
CLONE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"  # this clone (samples/kmp-project-template)

# ── 0. Pre-flight ────────────────────────────────────────────────────────────
gh auth status >/dev/null
gh repo view "$UPSTREAM" --json defaultBranchRef --jq '.defaultBranchRef.name' >/dev/null
[ -f "$CLONE_DIR/PR_README.md" ] || { echo "PR_README.md missing — run Phase 6 first"; exit 1; }
[ -f "$CLONE_DIR/UPSTREAM_SHA.txt" ] || { echo "UPSTREAM_SHA.txt missing — run Phase 1 first"; exit 1; }
PINNED_SHA=$(grep -oE '[0-9a-f]{40}' "$CLONE_DIR/UPSTREAM_SHA.txt")
echo "→ This clone pinned at upstream SHA: $PINNED_SHA"

# ── 1. Fork upstream (idempotent — gh handles already-forked) ────────────────
echo "→ Forking $UPSTREAM → $FORK_OWNER/kmp-project-template"
gh repo fork "$UPSTREAM" --clone=false --remote=false --org="$FORK_OWNER" 2>&1 \
    | grep -v 'already exists' || true

# ── 2. Clone the fork to a temp dir ─────────────────────────────────────────
echo "→ Cloning fork to $WORK_DIR"
git clone --depth=1 "https://github.com/${FORK_OWNER}/kmp-project-template.git" "$WORK_DIR"
cd "$WORK_DIR"
git remote add upstream "https://github.com/${UPSTREAM}.git"
git fetch upstream "$UPSTREAM_DEFAULT_BRANCH"

# ── 3. Reset fork to upstream's pinned SHA so we diff against a known base ──
# This avoids merging un-related fork drift. We force-checkout the same SHA
# our clone was cut from.
git checkout -B "$FEAT_BRANCH" "$PINNED_SHA"

# ── 4. rsync the additive deltas from our clone into the fork ───────────────
# EXCLUDES: clone-local marker files that don't belong in the upstream PR.
echo "→ Copying additive deltas from $CLONE_DIR → fork tree"
rsync -av --progress \
    --exclude='UPSTREAM_SHA.txt' \
    --exclude='.no-ci' \
    --exclude='PR_README.md' \
    --exclude='PR_URL.txt' \
    --exclude='PHASE7_RAISE_PR.sh' \
    --exclude='build/' \
    --exclude='.gradle/' \
    --exclude='.git/' \
    "$CLONE_DIR/" "$WORK_DIR/"

# ── 5. Sanity check — confirm key files landed ──────────────────────────────
echo "→ Sanity check"
test -f "$WORK_DIR/sync/build.gradle.kts" || { echo "❌ sync/build.gradle.kts missing"; exit 1; }
test -f "$WORK_DIR/build-logic/convention/src/main/kotlin/WorkerComposeConventionPlugin.kt" || { echo "❌ convention plugin missing"; exit 1; }
test -f "$WORK_DIR/core/data/src/commonMain/kotlin/org/mifos/core/data/Synchronizer.kt" || { echo "❌ Synchronizer.kt missing"; exit 1; }
grep -q ':sync' "$WORK_DIR/settings.gradle.kts" || { echo "❌ :sync not in settings"; exit 1; }
echo "  ✓ all key files present"

# ── 6. Commit + push ────────────────────────────────────────────────────────
git add -A
git commit -m "feat(sync): worker-kmp-backed background sync module

Adds a sync/ Gradle module that wires worker-kmp as the template's cross-platform
background sync solution. Single DataSyncWorker constructor-injects CurrencyRepository
(Frankfurter) + MacroIndicatorsRepository (WorldBank); WorkScheduler Koin facade
exposes 7 scheduling methods (one-time, periodic, daily-at-time, exact-time, notification).

Mirrors android/nowinandroid:sync architecture (verbatim Synchronizer + Syncable +
changeListSync) with snapshotSync sibling for snapshot APIs.

See PR description for full design + testing notes."

git push -u origin "$FEAT_BRANCH"

# ── 7. Open PR ──────────────────────────────────────────────────────────────
echo "→ Opening PR against $UPSTREAM:$UPSTREAM_DEFAULT_BRANCH"
PR_URL=$(gh pr create \
    --repo "$UPSTREAM" \
    --head "${FORK_OWNER}:${FEAT_BRANCH}" \
    --base "$UPSTREAM_DEFAULT_BRANCH" \
    --title "Add sync/ module backed by worker-kmp (cross-platform background sync)" \
    --body-file "$CLONE_DIR/PR_README.md")

echo "$PR_URL" > "$CLONE_DIR/PR_URL.txt"
echo
echo "═════════════════════════════════════════════════════════════════"
echo "  ✅ PR OPEN: $PR_URL"
echo "  URL saved to: $CLONE_DIR/PR_URL.txt"
echo "═════════════════════════════════════════════════════════════════"

# Cleanup
cd "$CLONE_DIR"
rm -rf "$WORK_DIR"
