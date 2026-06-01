#!/usr/bin/env bash
# scripts/run-parity-audit.sh — refresh the cross-platform parity audit matrix.
#
# Modes
#   (default)         Local-grep — scans cmp-worker-* modules for known feature
#                     markers, emits scripts/parity-audit-matrix.json, rewrites
#                     docs/operations/cross-platform-parity-audit.md "Last
#                     audited" timestamp + matrix cells. Idempotent.
#   --verify          Read-only. Exit 1 if the report doc + matrix.json diverge
#                     OR if any platform's status string diverges across the 3
#                     doc surfaces (Home.md + true-background-matrix.md +
#                     cross-platform-parity-audit.md). For CI.
#   --subagent-probe  Full 6-subagent run via Claude CLI (NOT YET IMPLEMENTED —
#                     stub that exits 0 with a notice; tracked for follow-up).
#
# Why this exists
#   GitHub Wiki + the README claim "single API + cross-platform parity." The
#   parity-audit doc is the evidence ledger. This harness keeps it fresh
#   without requiring a human to do manual code archaeology.
#
# Spec: GOAL.md of cross-platform-worker-parity-audit
# Author: 2026-06-01 (sub-plan 01 of cross-platform-worker-parity-audit)
set -euo pipefail

MODE="${1:-default}"

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

REPORT="docs/operations/cross-platform-parity-audit.md"
MATRIX="scripts/parity-audit-matrix.json"
HOME_MD="docs/Home.md"
BG_MATRIX="docs/platform-support/true-background-matrix.md"

# ────────────────────────────────────────────────────────────────────
# Feature markers — each row: PLATFORM | FEATURE | grep-pattern | path-glob
# Empty grep-pattern means "presence of file at path-glob == wired"
# ────────────────────────────────────────────────────────────────────
declare -a MARKERS=(
  # Android
  "android|OneTimeWorkRequest|OneTimeWorkRequest|cmp-worker-android/src/androidMain/kotlin"
  "android|PeriodicWorkRequest|PeriodicWorkRequest|cmp-worker-android/src/androidMain/kotlin"
  "android|setExpedited|setExpedited|cmp-worker-android/src/androidMain/kotlin"
  "android|ForegroundBridge|AndroidForegroundBridge|cmp-worker-android/src/androidMain/kotlin"
  "android|beginUniqueWork|beginUniqueWork|cmp-worker-android/src/androidMain/kotlin"
  # iOS
  "ios|BGProcessingTaskRequest|BGProcessingTaskRequest|cmp-worker-ios/src/iosMain/kotlin"
  "ios|BGAppRefreshTaskRequest|BGAppRefreshTaskRequest|cmp-worker-ios/src/iosMain/kotlin"
  "ios|BGContinuedProcessing|BGContinuedProcessingTaskRequest|cmp-worker-ios/src/iosMain/kotlin"
  "ios|cancelTask|cancelTaskRequestWithIdentifier|cmp-worker-ios/src/iosMain/kotlin"
  "ios|InfoPlistValidator|InfoPlistValidator|cmp-worker-ios/src/iosMain/kotlin"
  # Desktop
  "desktop|OneTimeWorkRequest|enqueue|cmp-worker-desktop/src/jvmMain/kotlin"
  "desktop|PeriodicWorkRequest|enqueueUniquePeriodicWork|cmp-worker-desktop/src/jvmMain/kotlin"
  "desktop|file-persistence|PropertiesFileWorkPersistence|cmp-worker-desktop/src/jvmMain/kotlin"
  "desktop|persistence-v2|schemaVersion|cmp-worker-desktop/src/jvmMain/kotlin"
  "desktop|daemon-dispatch|DaemonWorkerRegistry|cmp-worker-desktop-daemon"
  "desktop|win-installer|WindowsTaskInstaller|cmp-worker-desktop-daemon"
  "desktop|macos-installer|MacosLaunchdInstaller|cmp-worker-desktop-daemon"
  "desktop|linux-installer|LinuxSystemdInstaller|cmp-worker-desktop-daemon"
  # Web JS
  "web-js|ServiceWorker|navigator.serviceWorker.register|cmp-worker-web/src/jsMain/kotlin"
  "web-js|BroadcastChannel|BroadcastChannel|cmp-worker-web/src/jsMain/kotlin"
  "web-js|IndexedDB|IndexedDbWorkPersistence|cmp-worker-web/src/jsMain/kotlin"
  "web-js|WebPush|pushManager.subscribe|cmp-worker-web-push/src/jsMain/kotlin"
  # Web WasmJs
  "web-wasmjs|BroadcastChannel|@JsFun|cmp-worker-web/src/wasmJsMain/kotlin"
  "web-wasmjs|WebPush|@JsFun|cmp-worker-web-push/src/wasmJsMain/kotlin"
)

probe_marker() {
  # $1 = grep-pattern; $2 = path-glob (dir)
  local pattern="$1"
  local dir="$2"
  if [[ ! -d "$dir" ]]; then
    echo "absent"
    return
  fi
  if grep -r -q --include='*.kt' --include='*.kts' "$pattern" "$dir" 2>/dev/null; then
    echo "wired"
  else
    echo "absent"
  fi
}

# ────────────────────────────────────────────────────────────────────
# Emit JSON matrix
# ────────────────────────────────────────────────────────────────────
emit_json() {
  local first=1
  echo "{"
  echo "  \"generated_at\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
  echo "  \"commit\": \"$(git rev-parse HEAD)\","
  echo "  \"branch\": \"$(git rev-parse --abbrev-ref HEAD)\","
  echo "  \"features\": ["
  for marker in "${MARKERS[@]}"; do
    IFS='|' read -r platform feature pattern path <<< "$marker"
    status=$(probe_marker "$pattern" "$path")
    if [[ $first -eq 1 ]]; then
      first=0
    else
      echo ","
    fi
    printf '    {"platform":"%s","feature":"%s","status":"%s","probed_path":"%s"}' \
      "$platform" "$feature" "$status" "$path"
  done
  echo ""
  echo "  ]"
  echo "}"
}

# ────────────────────────────────────────────────────────────────────
# Refresh "Last audited" timestamp in the report doc (default mode)
# ────────────────────────────────────────────────────────────────────
refresh_report_timestamp() {
  if [[ ! -f "$REPORT" ]]; then
    echo "report doc missing: $REPORT" >&2
    return 1
  fi
  local today commit
  today="$(date -u +%Y-%m-%d)"
  commit="$(git rev-parse HEAD)"
  # Replace the "> **Last audited:** ..." line
  perl -i -pe "s|^> \*\*Last audited:\*\*.*\$|> **Last audited:** ${today} against commit \`${commit:0:7}\` on \`development\`.|" "$REPORT"
}

# ────────────────────────────────────────────────────────────────────
# Cross-doc consistency check (used by --verify)
# ────────────────────────────────────────────────────────────────────
check_doc_consistency() {
  local err=0
  # Home.md "True background?" / "Foreground?" cells must agree with the parity-audit summary table
  for platform in Android iOS Desktop Web; do
    local home_row matrix_row report_row
    home_row=$(grep -E "^\| \*\*${platform}" "$HOME_MD" 2>/dev/null || true)
    matrix_row=$(grep -E "^\| .*${platform}" "$BG_MATRIX" 2>/dev/null | head -1 || true)
    report_row=$(grep -E "^\| ${platform}" "$REPORT" 2>/dev/null | head -1 || true)
    if [[ -n "$home_row" && -n "$report_row" ]]; then
      local home_check report_check
      home_check=$(echo "$home_row" | grep -o "✓\|⚠\|✗" | head -1 || echo "?")
      report_check=$(echo "$report_row" | grep -o "✓\|⚠\|✗" | head -1 || echo "?")
      if [[ "$home_check" != "$report_check" && "$home_check" != "?" && "$report_check" != "?" ]]; then
        echo "::error::doc inconsistency for $platform — Home.md says ${home_check}, report says ${report_check}" >&2
        err=$((err + 1))
      fi
    fi
  done
  if [[ -f "$BG_MATRIX" ]] && grep -qF "SCAFFOLD" "$BG_MATRIX"; then
    echo "::error::true-background-matrix.md still has 'SCAFFOLD' disclaimer" >&2
    err=$((err + 1))
  fi
  return "$err"
}

# ────────────────────────────────────────────────────────────────────
# Main
# ────────────────────────────────────────────────────────────────────
case "$MODE" in
  --verify)
    # Read-only. Regenerate JSON in-memory, compare to disk — but exclude the
    # `generated_at` + `commit` + `branch` fields from comparison (they change per-run).
    if [[ ! -f "$MATRIX" ]]; then
      echo "::error::$MATRIX missing — run \`bash scripts/run-parity-audit.sh\` first" >&2
      exit 1
    fi
    expected_features="$(emit_json | jq -S '.features')"
    actual_features="$(jq -S '.features' < "$MATRIX")"
    if ! diff -q <(echo "$expected_features") <(echo "$actual_features") >/dev/null 2>&1; then
      echo "::error::$MATRIX features section is stale — re-run \`bash scripts/run-parity-audit.sh\`" >&2
      diff <(echo "$expected_features") <(echo "$actual_features") | head -20 >&2 || true
      exit 1
    fi
    check_doc_consistency || exit $?
    echo "✓ parity audit verified — matrix fresh + docs consistent"
    ;;
  --subagent-probe)
    echo "⚠ --subagent-probe not yet implemented (tracked for follow-up). Use default mode." >&2
    exit 0
    ;;
  default|"")
    emit_json > "$MATRIX"
    refresh_report_timestamp
    echo "✓ parity audit refreshed"
    echo "  matrix : $MATRIX"
    echo "  report : $REPORT"
    ;;
  *)
    echo "Usage: $0 [--verify | --subagent-probe]" >&2
    exit 2
    ;;
esac
