package io.github.mobilebytelabs.worker.config

import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkObserver

/**
 * Unified configuration for worker-kmp across all platforms.
 *
 * Added in v3.0.0-alpha00 (Phase 0). Per-platform sub-objects added in
 * v3.0.0-alpha00.X (Phase 0 deep refactor) — these carry the platform-specific
 * knobs that previously lived in IosWorkManagerConfig / DesktopWorkManagerConfig /
 * WebWorkManagerConfig / KmpAndroidWorkerFactory constructor args.
 *
 * Consumer apps now configure worker-kmp from a single commonMain entry point —
 * platform factories (from the per-platform `cmp-worker-android` / `-ios` / `-desktop` /
 * `-web` modules) consume their respective sub-config via
 * [io.github.mobilebytelabs.worker.WorkManagerFactory].
 */
public data class WorkerConfig(
    public val logLevel: LogLevel = LogLevel.WARN,
    public val defaultRetryConfig: RetryConfig = RetryConfig.DEFAULT,
    public val observers: List<WorkObserver> = emptyList(),
    public val androidConfig: AndroidWorkerConfig = AndroidWorkerConfig(),
    public val iosConfig: IosWorkerConfig = IosWorkerConfig(),
    public val desktopConfig: DesktopWorkerConfig = DesktopWorkerConfig(),
    public val webConfig: WebWorkerConfig = WebWorkerConfig(),
)

/**
 * Logging verbosity for worker-kmp internal logs (kermit-backed).
 *
 * Added in v3.0.0-alpha00.
 */
public enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, SILENT;
}

/**
 * Android-specific worker configuration.
 *
 * Added in v3.0.0-alpha00.X (Phase 0 deep refactor). Lives in commonMain so consumers
 * declare platform knobs from shared code; the cmp-worker-android module reads these
 * fields when constructing the platform WorkManager.
 *
 * @param notificationChannelId Optional notification channel id used by foreground workers
 *   that surface progress through Android's notification system. When null the channel is
 *   created on demand by androidx.work.
 * @param notificationChannelName Optional human-readable channel name shown in system UI.
 * @param useReflectionFactory When true (default), workers are instantiated via reflection
 *   from their FQCN. Set to false when wiring a custom [WorkerRegistry] that drives
 *   instantiation explicitly — useful for DI-friendly construction without forcing the
 *   reflective fallback.
 */
public data class AndroidWorkerConfig(
    public val notificationChannelId: String? = null,
    public val notificationChannelName: String? = null,
    public val useReflectionFactory: Boolean = true,
)

/**
 * iOS-specific worker configuration.
 *
 * Added in v3.0.0-alpha00.X (Phase 0 deep refactor). Mirrors the legacy
 * `IosWorkManagerConfig` field-by-field but lives in commonMain so consumers
 * declare iOS knobs from shared code.
 *
 * @param enableBackgroundTasks When true, registers a BGProcessingTask handler + schedules a
 *   background wake-up when constrained work is waiting. Requires
 *   [bgProcessingTaskIdentifier] to be set and the same identifier listed under
 *   `BGTaskSchedulerPermittedIdentifiers` in the host app's `Info.plist`.
 * @param bgProcessingTaskIdentifier BGProcessingTask identifier to register and schedule.
 *   Must match an entry in `Info.plist → BGTaskSchedulerPermittedIdentifiers`.
 * @param enablePersistence When true, work state is written to NSUserDefaults so pending
 *   work survives foreground/background transitions and app restarts.
 * @param persistenceKey NSUserDefaults key for stored work items. Override when multiple
 *   app extensions share the same NSUserDefaults suite to prevent collisions.
 */
public data class IosWorkerConfig(
    public val enableBackgroundTasks: Boolean = false,
    public val bgProcessingTaskIdentifier: String = "",
    public val enablePersistence: Boolean = true,
    public val persistenceKey: String = "worker-kmp-ios",
    /**
     * BGAppRefreshTask identifier used by periodic workers that opt-in via
     * `PeriodicWorkRequestBuilder.setQuickRefresh(true)`. Must match an entry in
     * `Info.plist → BGTaskSchedulerPermittedIdentifiers` AND `UIBackgroundModes`
     * must include `"fetch"`.
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    public val appRefreshTaskIdentifier: String = "",
)

/**
 * Desktop-specific worker configuration.
 *
 * Added in v3.0.0-alpha00.X (Phase 0 deep refactor). Mirrors the legacy
 * `DesktopWorkManagerConfig` but lives in commonMain so consumers declare Desktop
 * knobs from shared code.
 *
 * @param maxConcurrentWorkers Maximum parallel worker coroutines. Default 4 matches the
 *   legacy default.
 * @param persistenceEnabled When true, work state is persisted to [persistencePath].
 * @param persistencePath Filesystem directory for persistent work state. `null` means
 *   `~/.worker-kmp` resolved against `System.getProperty("user.home")` at construction time —
 *   the resolution is platform-side because `commonMain` can't reference `java.io.File` or
 *   the user-home property.
 * @param constraintCheckIntervalMs Polling interval for constraint evaluation.
 */
public data class DesktopWorkerConfig(
    public val maxConcurrentWorkers: Int = 4,
    public val persistenceEnabled: Boolean = true,
    public val persistencePath: String? = null,
    public val constraintCheckIntervalMs: Long = 5_000L,
    /**
     * Optional desktop background-daemon installation config. When non-null AND
     * [DesktopBackgroundConfig.installOnFirstRun] is true, the JVM `desktopWorkManagerFactory(...)`
     * builder invokes the daemon installer (`cmp-worker-desktop-daemon`) reflectively at first
     * construction — installing the OS-level scheduled task / launchd agent / systemd-user timer
     * that wakes the daemon to drain pending work while the consumer app is not running.
     *
     * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X). Auto-install is best-effort:
     * `cmp-worker-desktop-daemon` is NOT a hard dependency of `cmp-worker-desktop`, so when the
     * daemon module isn't on the classpath the auto-install hook logs a WARN and proceeds without
     * the background path (the foreground/in-process work flow continues to function normally).
     */
    public val background: DesktopBackgroundConfig? = null,
)

/**
 * Web-specific worker configuration.
 *
 * Added in v3.0.0-alpha00.X (Phase 0 deep refactor). Mirrors the legacy
 * `WebWorkManagerConfig` but lives in commonMain so consumers declare Web knobs
 * from shared code.
 *
 * @param enablePersistence When false, IndexedDB persistence is skipped (work survives
 *   only for the current page session).
 * @param persistenceDbName IndexedDB database name. Override when multiple apps share the
 *   same origin.
 * @param constraintCheckIntervalMs Polling interval for constraint evaluation.
 * @param enableBackgroundSync When true, registers a Browser Background Sync tag when
 *   network-constrained work is waiting. Requires a Service Worker at
 *   [serviceWorkerScript]. Falls back to polling when the API is unavailable.
 * @param serviceWorkerScript Path (relative to origin) where the worker-kmp Service Worker
 *   script is served. Only used when [enableBackgroundSync] is true.
 */
public data class WebWorkerConfig(
    public val enablePersistence: Boolean = true,
    public val persistenceDbName: String = "worker-kmp",
    public val constraintCheckIntervalMs: Long = 5_000L,
    public val enableBackgroundSync: Boolean = false,
    public val serviceWorkerScript: String = "/worker-kmp-sw.js",
    /**
     * Set to `true` to register a Browser Periodic Background Sync tag for periodic
     * workers. Requires a Service Worker at [serviceWorkerScript] AND the origin to be
     * a PWA installed by the user (browsers gate the API behind install heuristics).
     * Falls back to polling/timer when the API is unavailable.
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    public val enablePeriodicBackgroundSync: Boolean = false,
)
