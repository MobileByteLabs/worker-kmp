package io.github.mobilebytelabs.worker.app.gradle

import kotlinx.serialization.Serializable

/**
 * Mirror of `io.github.mobilebytelabs.worker.app.ksp.CodegenModel` — duplicated
 * here because the Gradle plugin module can't depend on the JVM-only KSP module
 * without creating a dependency cycle at the worker-kmp build level. JSON shape
 * is identical; if a field is added in the KSP model, mirror it here.
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
    val contentFnFqn: String,
) {
    val koinModulesFnSimpleName: String get() = koinModulesFnFqn.substringAfterLast('.')
    val contentFnSimpleName: String get() = contentFnFqn.substringAfterLast('.')
}
