package io.github.mobilebytelabs.worker

/**
 * Platform-specific context bridge. On Android this is a `typealias` to
 * `android.content.Context`; on iOS/Desktop/Web/Wasm it's an empty sentinel class.
 *
 * Used by [io.github.mobilebytelabs.worker.config.WorkerConfig] / Koin module wiring
 * to obtain the platform context without forcing consumers to import Android types
 * in commonMain.
 *
 * Added in v3.0.0-alpha00 (Phase 0).
 */
public expect class PlatformContext
