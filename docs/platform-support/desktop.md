---
title: "Desktop"
description: "worker-kmp on JVM Desktop — coroutine-based scheduler; no OS-level persistence, runs while process is alive."
---

# Desktop (JVM)

worker-kmp on Desktop runs in two modes:

1. **In-process** (default) — workers execute on a coroutine-backed scheduler inside the
   consumer app. Survives app lifetime but not app close.
2. **OS-scheduler daemon** (`cmp-worker-desktop-daemon`) — a separate JVM daemon registered
   with the OS scheduler (Windows Task Scheduler / macOS launchd / Linux systemd-user)
   that runs work even when the consumer app is closed AND survives reboot.

## Requirements

- JDK 11+ (JDK 17+ recommended)
- Windows 10+ / macOS 11+ / Linux with systemd (for daemon mode)

## Dependency

```kotlin
// commonMain
api(libs.worker.kmp)
api(libs.worker.koin)

// jvmMain
api(libs.worker.desktop)

// (optional) OS-scheduler daemon for true-background
implementation(libs.worker.desktop.daemon)
```

## Desktop setup (v4 single-API, in-process)

Worker wiring lives in your **commonMain** `initApp()` via `WorkerKmpAuto.install()` (see the
[Quick start](../getting-started/quick-start.md)); the generated Desktop installer selects
`desktopWorkManagerFactory()` for you. The Desktop `main()` just calls `initApp()`:

```kotlin
// desktopMain/main.kt
fun main() {
    initApp()               // commonMain initApp calls WorkerKmpAuto.install()
    // ... start your Compose Desktop UI
}
```

Desktop-specific `WorkerConfig` (e.g. a custom persistence dir) is supplied via a Koin binding —
see [Configuration](../wiki/single-api-guide.md#configuration):

```kotlin
// commonMain (or desktopMain) module wired into initApp's startKoin:
single {
    WorkerConfig(
        logLevel = LogLevel.INFO,
        desktopConfig = DesktopWorkManagerConfig(
            persistenceDir = Path.of(System.getProperty("user.home"), ".myapp", "work"),
        ),
    )
}
```

## Daemon mode (true background)

For tasks that must run when the app is closed AND across reboots:

```kotlin
// At install/setup time, install the daemon:
DesktopDaemonInstaller.install(
    appId = "com.example.myapp",
    daemonJarPath = "/path/to/my-daemon.jar",
    scheduleEveryMinutes = 15,
)

// At app start, point the factory at the daemon's persistence dir:
factory = desktopWorkManagerFactory(
    DesktopDaemonConfig(daemonId = "com.example.myapp"),
)
```

The installer writes:
- **Windows**: `schtasks /create` entry with TRIGGER `MINUTE` + per-app working directory
- **macOS**: `~/Library/LaunchAgents/com.example.myapp.plist` with `StartInterval`
- **Linux**: `~/.config/systemd/user/com.example.myapp.timer` + `.service` units

## Persistence

Work queue state lives in `DesktopWorkManagerConfig.persistenceDir` (default
`<user.home>/.workkmp/<app-id>/`). The format is HMAC-protected `.properties` files —
see [Security](../operations/security.md) T1-T6.

## See also

- [Platform API Matrix](platform-api-matrix.md)
- [True Background Matrix](true-background-matrix.md) — daemon mode vs other platforms
- [Security](../operations/security.md) — daemon integrity model + threat assumptions
