package io.github.mobilebytelabs.worker.registry

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkerContext

/**
 * Type-safe worker registry. Consumers declare workers in commonMain via [register];
 * platform actuals consult this registry to instantiate workers by class name without
 * reflection (works on iOS/JS/Wasm targets).
 *
 * Once a registry is loaded into Koin (via `workKoinModule`), it becomes immutable —
 * additional registrations throw [WorkerRegistryAlreadyLoadedException]. This defends
 * against late-registration races + injection attacks.
 *
 * Added in v3.0.0-alpha00 (Phase 0).
 *
 * Example:
 * ```kotlin
 * val registry = workerRegistry {
 *     register<SyncWorker> { ctx -> SyncWorker(ctx, koin.get<SyncRepository>()) }
 *     register<UploadWorker> { ctx -> UploadWorker(ctx, koin.get(), koin.get()) }
 * }
 * ```
 *
 * @see workerRegistry
 */
public class WorkerRegistry internal constructor() {

    private val factories: MutableMap<String, (WorkerContext) -> CoroutineWorker> = mutableMapOf()
    private var locked: Boolean = false

    /**
     * Register a worker factory keyed by the worker's simple class name.
     *
     * The simple-name key matches the convention used by [OneTimeWorkRequestBuilder] +
     * [PeriodicWorkRequestBuilder], so platform actuals can resolve workers from a
     * [WorkRequest.workerClass] string. Kotlin/JS does NOT support `qualifiedName` at
     * runtime, so this registry intentionally uses `simpleName` to stay portable across
     * iOS/JS/Wasm/JVM.
     *
     * @throws WorkerRegistryAlreadyLoadedException if invoked after Koin has loaded
     *   the registry (i.e. after workKoinModule(...) appears in startKoin's modules).
     */
    public inline fun <reified T : CoroutineWorker> register(
        noinline factory: (WorkerContext) -> T,
    ) {
        register(T::class.simpleName ?: error("Worker class must have simpleName"), factory)
    }

    public fun register(className: String, factory: (WorkerContext) -> CoroutineWorker) {
        validateClassName(className)
        if (locked) throw WorkerRegistryAlreadyLoadedException(className)
        factories[className] = factory
    }

    /**
     * Resolve a worker by FQCN. Intended for platform actuals + Koin integration —
     * application code should not call this directly.
     */
    public fun create(className: String, context: WorkerContext): CoroutineWorker? =
        factories[className]?.invoke(context)

    /**
     * Lock the registry against further registrations. Called by `workKoinModule(...)` in
     * `cmp-worker-koin` when the module is loaded. Application code should not call this
     * directly — registrations should always happen inside the `workerRegistry { ... }` block.
     */
    public fun lock() { locked = true }

    /**
     * The set of registered FQCNs. Intended for diagnostics + Koin integration.
     */
    public fun registeredClassNames(): Set<String> = factories.keys.toSet()

    private fun validateClassName(name: String) {
        // Defends against T22 (per SECURITY.md): reject path-injection and null bytes
        if (name.contains("..") || name.contains("/") || name.contains(" ")) {
            throw IllegalArgumentException("Invalid worker class name (rejected: ..,/, null byte): $name")
        }
    }
}

public fun workerRegistry(block: WorkerRegistry.() -> Unit): WorkerRegistry =
    WorkerRegistry().apply(block)

public class WorkerRegistryAlreadyLoadedException(
    public val attemptedRegistration: String,
) : IllegalStateException(
    "Cannot register worker '$attemptedRegistration' after the WorkerRegistry has been loaded into Koin. " +
        "Move all register<T>() calls into the workerRegistry { ... } block passed to workKoinModule(...).",
)
