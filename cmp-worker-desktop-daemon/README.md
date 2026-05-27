# cmp-worker-desktop-daemon

> Desktop true-background daemon for worker-kmp.

## Current state (alpha05.X — Phase 8 alpha05.X)

- ✓ Module + Gradle wiring
- ✓ `DesktopBackgroundDaemon.main()` — real loop: parse flags, install RotatingLogger,
  acquire LockFile, verify JAR integrity, scan persistence dir, heal stuck-RUNNING entries
- ✓ `DesktopBackgroundConfig` (moved to `cmp-worker-kmp` commonMain at
  `io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig` — now field of
  `DesktopWorkerConfig.background`)
- ✓ `DesktopBackgroundInstaller` interface
- ✓ Per-OS installer impls — `WindowsTaskInstaller` (schtasks), `MacosLaunchdInstaller`
  (launchctl + ~/Library/LaunchAgents plist), `LinuxSystemdInstaller` (systemctl --user
  + ~/.config/systemd/user units), `LinuxCronInstaller` (crontab fallback),
  `LinuxInstallerRouter` (selects systemd-first, falls back to cron)
- ✓ JAR integrity check (`JarIntegrityCheck` — SHA-256 of running JAR vs.
  `{persistenceDir}/daemon.jar.sha256` written at install time)
- ✓ LockFile single-instance guard (`FileChannel.tryLock` + PID in
  `{persistenceDir}/daemon.lock`)
- ✓ RotatingLogger (`{persistenceDir}/logs/daemon.log.{0..2}`, 1 MB × 3 files)
- ✓ HmacPersistence helpers (HMAC-SHA256 envelope; integration into
  PropertiesFileWorkPersistence deferred to alpha05.X.Y)
- ✓ Auto-install hook in `cmp-worker-desktop`'s `desktopWorkManagerFactory(...)` —
  invoked reflectively, no hard dep
- ✓ 6 smoke tests in `DesktopDaemonTest`
- 🕒 PropertiesFileWorkPersistence read/execute integration in daemon main loop
  — alpha05.X.Y (needs richer schema carrying `workerClass` FQCN + `inputData`)
- 🕒 ShadowJar fat-JAR packaging — alpha05.X.Y

## Architecture

See Phase 8 sub-plan body for the target architecture:
`plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/08-desktop-background-daemon.md`

## Coordinates

`io.github.mobilebytelabs:worker-desktop-daemon:3.0.0-alpha05.X` (Maven Central — pending
the alpha05.X publication).

## See also

- [docs/platform-support/true-background-matrix.md](../docs/platform-support/true-background-matrix.md)
- [docs/operations/security.md](../docs/operations/security.md) T1-T6 (daemon supply chain
  — Phase 10's extension)
