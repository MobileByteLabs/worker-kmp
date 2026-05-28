package io.github.mobilebytelabs.worker

/**
 * Marks a WorkManager API whose background execution support is not yet fully implemented
 * on the target platform. Work will only run while the app is in the foreground.
 *
 * Platforms affected: iOS, Web (JS/WasmJs)
 * Platforms with full background support: Android (via androidx.work)
 *
 * Opt in at the call site:
 * ```kotlin
 * @OptIn(ExperimentalWorkerApi::class)
 * fun initWorker() {
 *     startKoin { modules(workKoinModule(factory = iosWorkManagerFactory(), workers = registry)) }
 * }
 * ```
 */
@RequiresOptIn(
    message = "Background execution is not supported on this platform yet. " +
        "Work runs only while the app is in the foreground. " +
        "Opt in with @OptIn(ExperimentalWorkerApi::class) to acknowledge this limitation.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalWorkerApi
