package io.github.mobilebytelabs.worker.desktop

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import java.io.File

/**
 * Constructs a [WorkManagerFactory] that builds a [DesktopWorkManager] from the
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.desktopConfig] sub-object. Pass
 * the result as the `factory` parameter to `workKoinModule(...)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). REPLACES the legacy
 * `initializeWorkerDesktop(...)` side-effecting init function and the
 * `PlatformWorkManager.configure(...)` global slot — clean break, no v2-style auto-init.
 *
 * ```kotlin
 * fun main() {
 *     startKoin {
 *         modules(
 *             workKoinModule(
 *                 config = WorkerConfig(
 *                     desktopConfig = DesktopWorkerConfig(persistenceEnabled = false),
 *                 ),
 *                 workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *                 factory = desktopWorkManagerFactory(),
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * The factory threads the consumer's [WorkerRegistry] through a [DesktopWorkerFactory]
 * adapter — workers register once in commonMain. When
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.androidConfig.useReflectionFactory]
 * is not the desktop-specific knob (that's Android-only); on desktop, reflection is the
 * fallback when the registry returns null.
 */
public fun desktopWorkManagerFactory(): WorkManagerFactory = WorkManagerFactory { config, workers ->
    val persistencePath = config.desktopConfig.persistencePath?.let { File(it) }
        ?: File(System.getProperty("user.home")).resolve(".worker-kmp")
    // Best-effort daemon auto-install — opt-in via DesktopBackgroundConfig.installOnFirstRun.
    // The daemon module is NOT a hard dep of cmp-worker-desktop, so the call is reflective
    // and silently skips when cmp-worker-desktop-daemon isn't on the classpath.
    // Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
    config.desktopConfig.background?.let { bg ->
        if (bg.installOnFirstRun) {
            autoInstallDaemonIfRequested(bg)
        }
    }
    DesktopWorkManager(
        config = DesktopWorkManagerConfig(
            maxConcurrentWorkers = config.desktopConfig.maxConcurrentWorkers,
            persistenceEnabled = config.desktopConfig.persistenceEnabled,
            persistencePath = persistencePath,
            constraintCheckIntervalMs = config.desktopConfig.constraintCheckIntervalMs,
        ),
        workerFactory = ChainedDesktopWorkerFactory(workers, ReflectionWorkerFactory),
    )
}

/**
 * Reflectively invokes `cmp-worker-desktop-daemon`'s installer factory so the daemon module
 * stays a soft dependency. Steps:
 * 1. `Class.forName("...DesktopInstallerFactoryKt")` — fails fast with ClassNotFoundException
 *    when the daemon module isn't on the classpath; we WARN and proceed.
 * 2. `createDesktopBackgroundInstaller()` (static method on the Kt class) returns a
 *    `DesktopBackgroundInstaller`.
 * 3. `installer.isInstalled(appId)` — skip if already installed.
 * 4. `installer.install(bg)` — uses the consumer's appId / daemonJarPath / pollIntervalMin /
 *    runOnlyIfLoggedOn.
 *
 * All exceptions are caught and logged at WARN; a missing daemon module must never break
 * normal `desktopWorkManagerFactory(...)` construction.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
 */
private fun autoInstallDaemonIfRequested(bg: DesktopBackgroundConfig) {
    val log = Logger.withTag("worker-kmp.desktop")
    runCatching {
        val factoryCls = Class.forName(
            "io.github.mobilebytelabs.worker.daemon.installer.DesktopInstallerFactoryKt",
        )
        val createFn = factoryCls.getMethod("createDesktopBackgroundInstaller")
        val installer = createFn.invoke(null)
            ?: throw IllegalStateException("createDesktopBackgroundInstaller() returned null")
        val installerCls = installer.javaClass
        // isInstalled(String) -> Boolean
        val isInstalledFn = installerCls.methods.firstOrNull {
            it.name == "isInstalled" && it.parameterCount == 1
        } ?: throw NoSuchMethodException("DesktopBackgroundInstaller.isInstalled(String) not found")
        val already = isInstalledFn.invoke(installer, bg.appId) as? Boolean ?: false
        if (already) {
            log.i { "Daemon already installed for ${bg.appId}; skipping auto-install." }
            return@runCatching
        }
        // install(DesktopBackgroundConfig) -> InstallResult
        val installFn = installerCls.methods.firstOrNull {
            it.name == "install" && it.parameterCount == 1
        } ?: throw NoSuchMethodException("DesktopBackgroundInstaller.install(DesktopBackgroundConfig) not found")
        val result = installFn.invoke(installer, bg)
        log.i { "Daemon auto-install for ${bg.appId} → $result" }
    }.onFailure { e ->
        log.w {
            "Daemon auto-install skipped: ${e.javaClass.simpleName}: ${e.message}. " +
                "Add `cmp-worker-desktop-daemon` to the runtime classpath to enable true-background work."
        }
    }
}

/**
 * Consults [registry] for the worker class; on absence delegates to [fallback] which
 * uses reflection (`Class.forName`). Lets consumers mix registry-backed workers with
 * reflection-instantiated ones during incremental migration.
 */
internal class ChainedDesktopWorkerFactory(
    private val registry: WorkerRegistry,
    private val fallback: DesktopWorkerFactory,
) : DesktopWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context) ?: fallback.create(workerClass, context)
}
