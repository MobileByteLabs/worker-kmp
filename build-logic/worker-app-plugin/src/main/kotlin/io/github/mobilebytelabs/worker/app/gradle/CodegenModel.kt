package io.github.mobilebytelabs.worker.app.gradle

import kotlinx.serialization.Serializable

/**
 * Mirror of `io.github.mobilebytelabs.worker.app.ksp.CodegenModel` — duplicated
 * here because the Gradle plugin module can't depend on the JVM-only KSP module
 * without creating a dependency cycle at the worker-kmp build level. JSON shape
 * is identical; if a field is added in the KSP model, mirror it here.
 *
 * v4.0.0 (worker-kmp-single-api-completion): extended with `workers: List<WorkerDef>`
 * for codegen-driven worker registration (D21 + D31 + D23 — optional + within-module).
 */
@Serializable
internal data class CodegenModel(
    val title: String,
    val iosBundleId: String,
    val webCanvasId: String,
    val androidApplicationId: String,
    val androidPermissions: List<String>,
    val packageName: String,
    val koinModulesFnFqn: String,
    val koinModulesFnTakesFactory: Boolean = false,
    val contentFnFqn: String,
    val workers: List<WorkerDef> = emptyList(),
) {
    val koinModulesFnSimpleName: String get() = koinModulesFnFqn.substringAfterLast('.')
    val contentFnSimpleName: String get() = contentFnFqn.substringAfterLast('.')
}

@Serializable
internal data class WorkerDef(
    val fqn: String,
    val koinDeps: List<DepType>,
    val platformsFilter: Set<String> = emptySet(),
) {
    val simpleName: String get() = fqn.substringAfterLast('.')

    /** Returns true if this worker should be registered on [platform] (empty filter = all). */
    fun isOnPlatform(platform: String): Boolean = platformsFilter.isEmpty() || platform in platformsFilter
}

@Serializable
internal data class DepType(val paramName: String, val fqn: String, val namedQualifier: String? = null)
