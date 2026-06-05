package io.github.mobilebytelabs.worker.ios

import kotlin.test.Test

class InfoPlistValidatorTest {

    @Test
    fun validateInfoPlist_withDefaultConfig_doesNotThrow() {
        validateInfoPlist(IosWorkManagerConfig())
    }

    @Test
    fun validateInfoPlist_withBackgroundTasksEnabled_doesNotThrow() {
        val config = IosWorkManagerConfig(
            enableBackgroundTasks = true,
            bgProcessingTaskIdentifier = "com.example.sync",
        )
        validateInfoPlist(config)
    }

    @Test
    fun validateInfoPlist_withAppRefreshIdentifier_doesNotThrow() {
        val config = IosWorkManagerConfig(
            appRefreshTaskIdentifier = "com.example.refresh",
        )
        validateInfoPlist(config)
    }
}
