package io.github.mobilebytelabs.worker.app

/**
 * Marks the top-level Koin-module-provider function for a worker-kmp app.
 *
 * Consumer usage (in commonMain):
 *
 * ```
 * @WorkerKmpApp(
 *     title = "My App",
 *     iosBundleId = "com.example.myapp",
 *     webCanvasId = "composeCanvas",
 *     androidPermissions = ["android.permission.POST_NOTIFICATIONS"],
 * )
 * fun appKoinModules(factory: WorkManagerFactory): List<Module> = listOf(
 *     appModule(),
 *     workModule(factory),
 * )
 * ```
 *
 * The build-time `cmp-worker-app-ksp` processor finds this annotation,
 * validates the function shape, and emits a `CodegenModel` JSON that
 * `cmp-worker-app-plugin` reads to generate per-platform launcher files
 * (Android Application + Activity, JVM `fun main`, iOS `MainViewController`,
 * wasmJs `fun main` + `index.html`) into the consumer's build directory.
 *
 * Contract on the annotated function:
 *  - MUST be top-level (not inside a class).
 *  - MUST live in commonMain.
 *  - MUST have signature `(WorkManagerFactory) -> List<Module>`.
 *  - MUST be the SOLE `@WorkerKmpApp`-annotated function in the compilation.
 *
 * @property title Display title used as the Desktop window title, the iOS
 *   `CFBundleDisplayName`, and the Web `<title>` tag.
 * @property iosBundleId Bundle identifier for the iOS app — written to the
 *   generated `iosApp/iosApp/Info.plist` + xcodegen `project.yml`.
 * @property webCanvasId DOM id of the viewport container in the generated
 *   wasmJs `index.html` (default `composeCanvas`); the generated
 *   `Generated_Main.kt` calls `launchWebWorkerApp(canvasElementId = ...)`
 *   with this value.
 * @property androidApplicationId Android `applicationId` for the generated
 *   sample app. Empty string means "derive from the consumer's gradle
 *   namespace" (plugin auto-resolves at build time).
 * @property androidPermissions Additional Android permissions injected into
 *   the generated `AndroidManifest.xml` `<uses-permission>` entries (the
 *   plugin manifest-merges this with consumer's own manifest).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
public annotation class WorkerKmpApp(
    val title: String,
    val iosBundleId: String,
    val webCanvasId: String = "composeCanvas",
    val androidApplicationId: String = "",
    val androidPermissions: Array<String> = [],
)
