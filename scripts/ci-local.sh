#!/usr/bin/env bash
# ci-local.sh — Run CI checks locally before pushing.
# Delegates standard checks to the framework-level ci-local.sh
# (mbl-actionhub v1.1.0 fast gate), then layers worker-kmp-specific checks
# (iOS sim build via local Xcode 26.x) for the --all path.
#
# Usage:
#   ./scripts/ci-local.sh                  # fast gate: quality + JVM + Linux (~3 min)
#   ./scripts/ci-local.sh --all            # + iOS sim build via local Xcode 26.x (~5-10 min)
#   ./scripts/ci-local.sh --module cmp-worker
#   ./scripts/ci-local.sh --act            # run via act (requires Docker)
#
# Why iOS sim is in this script + only informational in CI:
#   Compose Multiplatform 1.11.0's iOS framework auto-links iOS-18-only private
#   frameworks (UIUtilities, SwiftUICore) which require Xcode 26.x at link time.
#   GitHub Actions is migrating `macos-latest` from macOS 15 (Xcode 16.x) to
#   macOS 26 (Xcode 26.x) between June 15 → July 15, 2026 — until that
#   completes, the CI iOS sim job is informational (continue-on-error: true).
#   This local script IS the iOS sim source of truth during the migration window.

set -euo pipefail

# ── Locate framework root (walk up until FRAMEWORK_GRAPH.yaml found) ─────────
DIR="$(cd "$(dirname "$0")" && pwd)"
while [ "$DIR" != "/" ]; do
  [ -f "$DIR/FRAMEWORK_GRAPH.yaml" ] && break
  DIR="$(dirname "$DIR")"
done

FRAMEWORK_CI="${DIR}/layers/ci/scripts/ci-local.sh"

if [ ! -f "$FRAMEWORK_CI" ]; then
  echo "  [ERROR] Framework ci-local.sh not found at: $FRAMEWORK_CI"
  echo "  Ensure claude-product-cycle framework is set up."
  exit 1
fi

# Project root = one level up from scripts/ (reliable, no git dependency)
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# ── Detect --all flag (forwarded to framework, AND triggers extra iOS step) ──
WANT_IOS=0
for arg in "$@"; do
  if [ "$arg" = "--all" ]; then
    WANT_IOS=1
  fi
done

# ── Phase 1: framework-level checks (delegates to mbl-actionhub fast gate) ───
echo "▸ Phase 1: framework-level checks (quality + JVM + Linux)"
bash "$FRAMEWORK_CI" --project "$PROJECT_DIR" "$@"

# ── Phase 2: worker-kmp iOS sim build (only with --all, only on macOS) ───────
if [ "$WANT_IOS" -eq 0 ]; then
  echo ""
  echo "▸ Tip: pass --all to also run the iOS sim build via local Xcode."
  exit 0
fi

if [ "$(uname)" != "Darwin" ]; then
  echo ""
  echo "▸ Phase 2: SKIPPED iOS sim build — not on macOS"
  exit 0
fi

echo ""
echo "▸ Phase 2: iOS sim build via local Xcode (worker-kmp-specific)"

# Verify Xcode version >= 26.0 — required by Compose Multiplatform 1.11+
XCODE_VERSION="$(xcodebuild -version 2>/dev/null | head -1 | awk '{print $2}')"
if [ -z "$XCODE_VERSION" ]; then
  cat >&2 <<'EOF'
  [ERROR] xcodebuild not found or no full Xcode active.
  Required: Xcode 26.x (Compose Multiplatform 1.11+ auto-links iOS-18-only
  private frameworks that don't exist in Xcode 16.x SDK).

  Setup:
    1. xcodes install 26.5                                # ~10 GB, prompts for Apple ID
    2. sudo xcode-select --switch /Applications/Xcode-26.5.0.app/Contents/Developer
    3. sudo xcodebuild -license accept
    4. brew install xcodegen
    5. xcodebuild -downloadPlatform iOS                   # iOS Simulator runtime
EOF
  exit 1
fi

XCODE_MAJOR="${XCODE_VERSION%%.*}"
if [ "$XCODE_MAJOR" -lt 26 ]; then
  cat >&2 <<EOF
  [ERROR] Xcode $XCODE_VERSION is too old. Required: Xcode 26.x or later.

  Reason: Compose Multiplatform 1.11+ iOS framework auto-links iOS-18-only
  private frameworks (UIUtilities, SwiftUICore). These exist only in
  iPhoneSimulator26.x SDK (which ships with Xcode 26.x).

  Upgrade:
    xcodes install 26.5
    sudo xcode-select --switch /Applications/Xcode-26.5.0.app/Contents/Developer
EOF
  exit 1
fi

echo "  ✓ Xcode $XCODE_VERSION (major=$XCODE_MAJOR — meets ≥26 floor)"

# Verify xcodegen
if ! command -v xcodegen >/dev/null 2>&1; then
  echo "  [ERROR] xcodegen not found. Install with: brew install xcodegen" >&2
  exit 1
fi
echo "  ✓ xcodegen $(xcodegen --version 2>/dev/null | head -1)"

# Verify iOS Simulator SDK
if ! xcrun --sdk iphonesimulator --show-sdk-path >/dev/null 2>&1; then
  echo "  [ERROR] iOS Simulator SDK not installed." >&2
  echo "         Install: xcodebuild -downloadPlatform iOS" >&2
  exit 1
fi
echo "  ✓ iOS Sim SDK: $(xcrun --sdk iphonesimulator --show-sdk-path)"

cd "$PROJECT_DIR"

# 1. Build the Kotlin/Native iOS Simulator framework
echo "  → :samples:cmp-worker-sample-compose-store:linkDebugFrameworkIosSimulatorArm64"
./gradlew :samples:cmp-worker-sample-compose-store:linkDebugFrameworkIosSimulatorArm64

# 2. Generate the .xcodeproj from xcodegen project.yml
echo "  → xcodegen generate (iosApp/project.yml → iosApp/iosApp.xcodeproj)"
(cd samples/cmp-worker-sample-compose-store/iosApp && xcodegen generate)

# 3. xcodebuild iosApp against iOS Simulator (arm64 only)
echo "  → xcodebuild iosApp (Debug, iOS Simulator, arm64)"
xcodebuild \
  -project samples/cmp-worker-sample-compose-store/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  EXCLUDED_ARCHS= \
  build

echo ""
echo "✅ Phase 2: iOS sim build succeeded via local Xcode $XCODE_VERSION"
echo "✅ ci-local.sh complete."
