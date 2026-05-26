package io.github.mobilebytelabs.worker

actual object PlatformWorkManager {
    private var _delegate: WorkManager? = null

    actual operator fun invoke(): WorkManager = _delegate
        ?: error(
            "WorkManager not configured for iOS. " +
                "Add the worker-ios module to iosMain dependencies."
        )

    fun configure(workManager: WorkManager) {
        _delegate = workManager
    }
}
