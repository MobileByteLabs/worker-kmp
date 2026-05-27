package io.github.mobilebytelabs.worker

actual fun platformBackgroundCapabilities() = BackgroundCapabilities(
    supportsPersistence = true,
    supportsOsScheduling = false,
)
