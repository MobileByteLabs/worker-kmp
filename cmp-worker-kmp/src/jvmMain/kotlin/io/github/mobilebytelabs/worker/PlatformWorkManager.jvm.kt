package io.github.mobilebytelabs.worker

actual object PlatformWorkManager {
    private var _delegate: WorkManager? = null

    actual operator fun invoke(): WorkManager = _delegate
        ?: error(
            "WorkManager not configured for JVM/Desktop. " +
                "Add the worker-desktop module to jvmMain dependencies.",
        )

    fun configure(workManager: WorkManager?) {
        _delegate = workManager
    }
}
