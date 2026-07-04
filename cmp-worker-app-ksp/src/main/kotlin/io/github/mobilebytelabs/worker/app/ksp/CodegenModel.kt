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
    /**
     * True when the `@WorkerKmpApp`-annotated function takes a single `WorkManagerFactory`
     * parameter (the documented `(WorkManagerFactory) -> List<Module>` contract).
     * False when the function is no-arg (`() -> List<Module>`, v4.0.0 simplified form).
     * Codegen branches on this so per-platform launchers either call `koinFn()` or
     * `koinFn(platformWorkManagerFactory())` to match the consumer's signature.
     */
    val koinModulesFnTakesFactory: Boolean = false,
    /** Fully-qualified name of the @WorkerKmpAppContent function (e.g. `com.example.AppContent`). */
    val contentFnFqn: String,
    /**
     * Aggregated worker declarations from all `@WorkerKmpWorkers` annotation sites within
     * the module (D23 — within-module aggregation only). Sorted alphabetically by [WorkerDef.fqn]
     * for deterministic codegen output (D28). Empty list if no `@WorkerKmpWorkers` sites
     * (D31 — `@WorkerKmpWorkers` is optional).
     */
    val workers: List<WorkerDef> = emptyList(),
    /**
     * Whether the plugin should generate the per-platform application + launcher files
     * (Android `Application`/`Activity`, iOS `MainViewController` + xcodegen, Desktop/Web
     * `main()`).
     *
     * `true` — "Shape 1" full-app codegen: the consumer declared `@WorkerKmpApp` +
     * `@WorkerKmpAppContent`; all identity fields above are populated.
     *
     * `false` — "Shape 2" bring-your-own-Application (GitHub issue #51): the consumer
     * declared only `@WorkerKmpWorkers` and writes their own `Application`. The plugin
     * emits ONLY the worker registry (`Generated_WorkerKmpInit.kt`) + the `WorkerKmpAuto`
     * install shim; every launcher/identity field above is left empty and unused.
     */
    val appGeneration: Boolean = true,
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
