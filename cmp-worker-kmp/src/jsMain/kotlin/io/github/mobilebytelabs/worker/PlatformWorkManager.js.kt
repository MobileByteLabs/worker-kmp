package io.github.mobilebytelabs.worker

actual object PlatformWorkManager {
    private var _delegate: WorkManager? = null

    actual operator fun invoke(): WorkManager = _delegate
        ?: error(
            "WorkManager not configured for JS. " +
                "Add the worker-web module to jsMain dependencies.",
        )

    fun configure(workManager: WorkManager) {
        _delegate = workManager
    }
}
