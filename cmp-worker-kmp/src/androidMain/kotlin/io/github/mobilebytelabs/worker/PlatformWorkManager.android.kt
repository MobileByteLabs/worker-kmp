package io.github.mobilebytelabs.worker

actual object PlatformWorkManager {
    private var _delegate: WorkManager? = null

    actual operator fun invoke(): WorkManager = _delegate
        ?: error(
            "WorkManager not configured for Android. " +
                "Add the worker-android module to androidMain dependencies."
        )

    fun configure(workManager: WorkManager) {
        _delegate = workManager
    }
}
