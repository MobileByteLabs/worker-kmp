package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.PlatformWorkManager
import io.github.mobilebytelabs.worker.WorkManager
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module that provides [WorkManager] as a process-scoped singleton backed by
 * [PlatformWorkManager].
 *
 * Include in your application's Koin setup:
 * ```kotlin
 * startKoin {
 *     modules(workKoinModule, appModule)
 * }
 * ```
 *
 * **Important**: the platform-specific initialiser must run *before* [startKoin] so that
 * [PlatformWorkManager] is configured when the singleton is first resolved.
 *
 * | Platform | Init call |
 * |---|---|
 * | Android  | `initializeWorkerAndroid(context, workerFactory)` |
 * | iOS      | `initIosWorkManager(workerFactory, config)` |
 * | Desktop  | `initializeWorkerDesktop(config, workerFactory)` |
 * | Web      | `initWebWorkManager(workerFactory, config)` |
 *
 * ## Wiring workers with Koin
 *
 * Declare each worker as a Koin `factory` keyed by its class name. The worker factory bridge
 * delegates to Koin so workers receive their dependencies from the DI graph:
 *
 * ```kotlin
 * // commonMain / shared module
 * val appModule = module {
 *     single<SyncRepository> { SyncRepositoryImpl(get()) }
 * }
 *
 * // desktopMain
 * fun initDesktop(koin: Koin) {
 *     initializeWorkerDesktop(
 *         workerFactory = object : DesktopWorkerFactory {
 *             override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
 *                 when (workerClass) {
 *                     "SyncWorker" -> SyncWorker(context, koin.get())
 *                     else         -> error("Unknown worker: $workerClass")
 *                 }
 *         },
 *     )
 * }
 * ```
 *
 * ## Android / Hilt
 *
 * On Android you can also use Dagger/Hilt with a custom [AndroidWorkerFactory]:
 * ```kotlin
 * @HiltWorker
 * class SyncWorker @AssistedInject constructor(
 *     @Assisted context: WorkerContext,
 *     private val repo: SyncRepository,
 * ) : CoroutineWorker(context)
 *
 * @Module @InstallIn(SingletonComponent::class)
 * abstract class WorkerModule {
 *     @Binds abstract fun bindWorkManager(impl: AndroidWorkManager): WorkManager
 * }
 * ```
 */
val workKoinModule: Module = module {
    single<WorkManager> { PlatformWorkManager() }
}
