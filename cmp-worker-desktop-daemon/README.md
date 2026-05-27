# cmp-worker-desktop-daemon

> Desktop true-background daemon for worker-kmp. Lands at v3.0.0-alpha05.

## Current state (alpha05 scaffold)

- ✓ Module + Gradle wiring
- ✓ DesktopBackgroundDaemon main entry point (logs + exits cleanly)
- ✓ DesktopBackgroundConfig + DesktopBackgroundInstaller interface + StubInstaller
- ✓ Smoke tests (4/4 GREEN)
- 🕒 Per-OS installer impls (schtasks / launchctl / systemctl) — alpha05.X
- 🕒 JAR integrity check + HMAC persistence — alpha05.X
- 🕒 LockFile PID tracking — alpha05.X
- 🕒 PropertiesFileWorkPersistence loading + work execution — alpha05.X

## Architecture (target)

See architecture diagram in Phase 8 sub-plan body:
`plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/08-desktop-background-daemon.md`

## Coordinates

`io.github.mobilebytelabs:worker-desktop-daemon:3.0.0-alpha05` (Maven Central).

## See also

- TRUE_BACKGROUND_MATRIX.md
- SECURITY.md T1-T6 (daemon supply chain — Phase 10's extension)
