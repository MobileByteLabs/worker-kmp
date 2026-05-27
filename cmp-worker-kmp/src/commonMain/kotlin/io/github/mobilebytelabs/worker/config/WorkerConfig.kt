package io.github.mobilebytelabs.worker.config

import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkObserver

/**
 * Unified configuration for worker-kmp across all platforms.
 *
 * Added in v3.0.0-alpha00 (Phase 0). Allows consumer apps to configure worker-kmp
 * from a single commonMain entry point rather than per-platform init calls.
 *
 * Per-platform config sub-objects (androidConfig, iosConfig, etc.) carry the
 * platform-specific knobs that previously lived in IosWorkManagerConfig /
 * DesktopWorkManagerConfig / WebWorkManagerConfig. Those types remain as the
 * concrete platform types in v3.0.0-alpha00; this WorkerConfig is the
 * commonMain-discoverable surface.
 */
public data class WorkerConfig(
    public val logLevel: LogLevel = LogLevel.WARN,
    public val defaultRetryConfig: RetryConfig = RetryConfig.DEFAULT,
    public val observers: List<WorkObserver> = emptyList(),
)

/**
 * Logging verbosity for worker-kmp internal logs (kermit-backed).
 *
 * Added in v3.0.0-alpha00.
 */
public enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, SILENT;
}
