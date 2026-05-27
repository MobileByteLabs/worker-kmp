package io.github.mobilebytelabs.worker

actual fun platformBackgroundCapabilities() = BackgroundCapabilities(
    supportsPersistence = false,
    supportsOsScheduling = false,
)
