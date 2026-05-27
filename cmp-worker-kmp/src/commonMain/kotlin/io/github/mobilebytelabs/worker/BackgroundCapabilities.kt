package io.github.mobilebytelabs.worker

/**
 * Describes the background task capabilities available on the current platform.
 *
 * Obtain via [platformBackgroundCapabilities].
 *
 * @property supportsPersistence work state is written to durable storage and survives process restarts.
 * @property supportsOsScheduling the OS can wake the app or process to run pending work without
 *   the user foregrounding it (Android WorkManager, iOS BGTaskScheduler).
 */
data class BackgroundCapabilities(val supportsPersistence: Boolean, val supportsOsScheduling: Boolean)

/** Returns the [BackgroundCapabilities] of the current platform. */
expect fun platformBackgroundCapabilities(): BackgroundCapabilities
