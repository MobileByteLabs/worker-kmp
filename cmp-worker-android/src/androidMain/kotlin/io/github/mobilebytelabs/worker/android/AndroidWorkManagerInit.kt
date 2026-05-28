package io.github.mobilebytelabs.worker.android

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry

/**
 * Constructs a [WorkManagerFactory] that builds an [AndroidWorkManager] backed by the
 * given Android [Context]. Pass the result as the `factory` parameter to
 * `workKoinModule(...)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). REPLACES the legacy
 * `initializeWorkerAndroid(...)` side-effecting init function and the
 * `PlatformWorkManager.configure(...)` global slot — clean break, no v2-style auto-init.
 *
 * The returned factory:
 * 1. Initialises `androidx.work.WorkManager` with a [KmpWorkerFactory] that consults the
 *    consumer's [WorkerRegistry] (or falls back to reflection per
 *    [WorkerConfig.androidConfig.useReflectionFactory]).
 * 2. Constructs and returns an [AndroidWorkManager] wrapping the now-initialised
 *    `androidx.work.WorkManager` for the given [Context].
 *
 * ```kotlin
 * class WorkerSampleApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         startKoin {
 *             androidContext(this@WorkerSampleApp)
 *             modules(
 *                 workKoinModule(
 *                     config = WorkerConfig(...),
 *                     workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *                     factory = androidWorkManagerFactory(this@WorkerSampleApp),
 *                 ),
 *                 appModule,
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * Note: this disables the automatic WorkManager initializer. Add
 * `<provider android:name="androidx.startup.InitializationProvider" tools:node="remove" />`
 * to your manifest, or use `androidx.work.impl.WorkManagerInitializer` removal so manual
 * initialisation takes effect.
 */
public fun androidWorkManagerFactory(context: Context): WorkManagerFactory = WorkManagerFactory { config, workers ->
    val kmpFactory: KmpAndroidWorkerFactory = if (config.androidConfig.useReflectionFactory) {
        // Registry takes precedence; reflection serves as fallback for unregistered workers
        // so the legacy reflection path keeps working during incremental migration.
        ChainedAndroidWorkerFactory(workers, ReflectionKmpWorkerFactory)
    } else {
        // Strict mode: registry is the only source of worker instances. Unregistered
        // workers fail loudly rather than silently falling back to reflection.
        WorkerRegistryAdapter(workers)
    }
    WorkManager.initialize(
        context,
        Configuration.Builder()
            .setWorkerFactory(KmpWorkerFactory(kmpFactory))
            .build(),
    )
    AndroidWorkManager(context)
}

/**
 * Bridges a commonMain [WorkerRegistry] to the Android-specific [KmpAndroidWorkerFactory]
 * contract. Throws when the worker class is unregistered — used in strict mode where the
 * consumer has disabled the reflection fallback.
 */
internal class WorkerRegistryAdapter(internal val registry: WorkerRegistry) : KmpAndroidWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context)
            ?: error(
                "Worker '$workerClass' not registered in WorkerRegistry. " +
                    "Add it via workerRegistry { register<$workerClass> { ctx -> $workerClass(ctx) } }, " +
                    "or enable reflection fallback by setting WorkerConfig.androidConfig.useReflectionFactory = true.",
            )
}

/**
 * Consults [registry] for the worker class; on absence delegates to [fallback].
 * Lets consumers mix registry-backed workers with reflection-instantiated ones during
 * incremental migration. Default mode when
 * [WorkerConfig.androidConfig.useReflectionFactory] is true.
 */
internal class ChainedAndroidWorkerFactory(
    private val registry: WorkerRegistry,
    private val fallback: KmpAndroidWorkerFactory,
) : KmpAndroidWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context) ?: fallback.create(workerClass, context)
}
