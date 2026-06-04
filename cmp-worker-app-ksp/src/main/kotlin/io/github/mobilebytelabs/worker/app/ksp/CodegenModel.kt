package io.github.mobilebytelabs.worker.app.ksp

import kotlinx.serialization.Serializable

/**
 * Spec passed from the KSP processor to the Gradle plugin via a build-dir
 * JSON file. Captures everything the per-platform launcher generators need:
 *  - identity (title, bundle/canvas ids)
 *  - location of the consumer's annotated functions (FQNs)
 *  - any per-platform extras (Android permissions)
 *  - aggregated worker declarations from `@WorkerKmpWorkers` sites (v4.0.0+)
 *
 * Serialized to `build/generated/ksp/.../META-INF/worker-kmp-app/codegen-model.json`
 * by [WorkerKmpAppProcessor]; read by `WorkerKmpAppPlugin` codegen tasks.
 */
@Serializable
public data class CodegenModel(
    val title: String,
    val iosBundleId: String,
    val webCanvasId: String,
    val androidApplicationId: String,
    val androidPermissions: List<String>,
    /** Package of the @WorkerKmpApp-annotated function (also where generated files live). */
    val packageName: String,
    /** Fully-qualified name of the @WorkerKmpApp function (e.g. `com.example.appKoinModules`). */
    val koinModulesFnFqn: String,
    /** Fully-qualified name of the @WorkerKmpAppContent function (e.g. `com.example.AppContent`). */
    val contentFnFqn: String,
    /**
     * Aggregated worker declarations from all `@WorkerKmpWorkers` annotation sites within
     * the module (D23 — within-module aggregation only). Sorted alphabetically by [WorkerDef.fqn]
     * for deterministic codegen output (D28). Empty list if no `@WorkerKmpWorkers` sites
     * (D31 — `@WorkerKmpWorkers` is optional).
     */
    val workers: List<WorkerDef> = emptyList(),
)

/**
 * Per-worker registration data captured by KSP from a `@WorkerKmpWorkers(workers = [...])`
 * annotation argument + the worker class's primary constructor.
 *
 * @property fqn fully-qualified name of the worker class.
 * @property koinDeps non-default-valued constructor params (after the `WorkerContext` first
 *   param) — codegen emits `getKoin().get<{depType.fqn}>(qualifier = {depType.namedQualifier?})`
 *   for each.
 * @property platformsFilter set of platforms this worker is registered on. Sourced from
 *   the optional `@WorkerForPlatforms` co-annotation on the worker class. Empty set means
 *   "all 4 platforms" (default).
 */
@Serializable
public data class WorkerDef(
    val fqn: String,
    val koinDeps: List<DepType>,
    val platformsFilter: Set<String> = emptySet(),
)

/**
 * A Koin-injected dependency type for a worker's primary constructor.
 *
 * @property paramName the constructor parameter name (used to emit `paramName = getKoin().get(...)`).
 * @property fqn fully-qualified type name (used as `getKoin().get<fqn>()`).
 * @property namedQualifier optional Koin qualifier name — set if the param has a
 *   `@org.koin.core.annotation.Named("…")` annotation. Generic types (D33/M5) require this.
 */
@Serializable
public data class DepType(val paramName: String, val fqn: String, val namedQualifier: String? = null)
