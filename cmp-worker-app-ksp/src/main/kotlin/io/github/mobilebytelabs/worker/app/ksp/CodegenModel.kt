package io.github.mobilebytelabs.worker.app.ksp

import kotlinx.serialization.Serializable

/**
 * Spec passed from the KSP processor to the Gradle plugin via a build-dir
 * JSON file. Captures everything the per-platform launcher generators need:
 *  - identity (title, bundle/canvas ids)
 *  - location of the consumer's annotated functions (FQNs)
 *  - any per-platform extras (Android permissions)
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
)
