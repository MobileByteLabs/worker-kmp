package io.github.mobilebytelabs.worker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import io.github.mobilebytelabs.worker.WorkManager

/**
 * CompositionLocal that provides the app's [WorkManager] instance.
 *
 * Access via [LocalWorkManager.current] inside any composable wrapped
 * by [WorkManagerProvider]. Throws if no provider is present.
 */
val LocalWorkManager: ProvidableCompositionLocal<WorkManager> = compositionLocalOf {
    error(
        "No WorkManager found in composition. " +
            "Wrap your root composable with WorkManagerProvider { ... }.",
    )
}

/**
 * Makes a [WorkManager] available via [LocalWorkManager] for all descendant composables.
 *
 * Typically called once near the top of the composition tree (e.g. in `App()`), passing
 * the [WorkManager] resolved from Koin via `koinInject<WorkManager>()` or `get<WorkManager>()`.
 *
 * ```kotlin
 * WorkManagerProvider(workManager = koinInject<WorkManager>()) {
 *     MyScreen()
 * }
 * ```
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor) — the previous default of
 * `PlatformWorkManager()` was removed alongside the global slot pattern. Callers now
 * supply the [WorkManager] explicitly (typically from their DI graph).
 */
@Composable
fun WorkManagerProvider(workManager: WorkManager, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWorkManager provides workManager) {
        content()
    }
}

/**
 * Returns the [WorkManager] from the nearest [WorkManagerProvider].
 *
 * Shorthand for `LocalWorkManager.current` — prefer this for readability.
 */
@Composable
fun rememberWorkManager(): WorkManager = LocalWorkManager.current
