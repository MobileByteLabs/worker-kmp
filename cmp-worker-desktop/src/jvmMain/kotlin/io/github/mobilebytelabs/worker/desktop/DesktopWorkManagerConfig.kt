package io.github.mobilebytelabs.worker.desktop

import java.io.File

data class DesktopWorkManagerConfig(
    val maxConcurrentWorkers: Int = 4,
    val persistenceEnabled: Boolean = true,
    val persistencePath: File = File(System.getProperty("user.home")).resolve(".worker-kmp"),
    val constraintCheckIntervalMs: Long = 5_000L,
) {
    companion object {
        val DEFAULT = DesktopWorkManagerConfig()
        val IN_MEMORY = DesktopWorkManagerConfig(persistenceEnabled = false)
    }
}
