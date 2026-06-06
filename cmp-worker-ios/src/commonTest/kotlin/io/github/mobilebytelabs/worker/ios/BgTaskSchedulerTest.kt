package io.github.mobilebytelabs.worker.ios

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test

class BgTaskSchedulerTest {

    @Test
    fun registerBgProcessingTask_emptyIdentifier_isNoOp() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        registerBgProcessingTask("", scope) { true }
    }

    @Test
    fun scheduleBgProcessingTask_emptyIdentifier_isNoOp() {
        scheduleBgProcessingTask("", requiresNetwork = false, requiresCharging = false)
    }

    @Test
    fun scheduleBgAppRefreshTask_emptyIdentifier_isNoOp() {
        scheduleBgAppRefreshTask("", earliestBeginInSeconds = 0.0)
    }
}
