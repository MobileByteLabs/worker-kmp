package io.github.mobilebytelabs.worker.app

/**
 * Marks the root @Composable function for a worker-kmp app.
 *
 * Consumer usage (in commonMain):
 *
 * ```
 * @WorkerKmpAppContent
 * @Composable
 * fun AppContent() {
 *     val workManager: WorkManager = koinInject()
 *     val store: Store<String, Article> = koinInject()
 *     WorkManagerProvider(workManager) { ArticlesScreen(store) }
 * }
 * ```
 *
 * Contract:
 *  - MUST be top-level (not inside a class).
 *  - MUST live in commonMain.
 *  - MUST be `@Composable fun() : Unit` with no parameters — dependencies
 *    come from Koin via `koinInject()` / `koinViewModel()` calls inside.
 *  - MUST be the SOLE `@WorkerKmpAppContent`-annotated function in the
 *    compilation.
 *
 * The plugin's generated per-platform launchers all invoke this function
 * via `Generated_MainActivity.kt`, `Generated_Main.kt`, `Generated_MainViewController.kt`.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
public annotation class WorkerKmpAppContent
