package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.Constraints
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopConstraintEvaluatorTest {

    @Test
    fun evaluate_noConstraints_returnsTrue() = runTest {
        val evaluator = DesktopConstraintEvaluator(DesktopWorkManagerConfig.IN_MEMORY)
        assertTrue(evaluator.evaluate(Constraints.NONE))
    }

    @Test
    fun evaluator_constructsWithConfig() {
        val config = DesktopWorkManagerConfig(maxConcurrentWorkers = 2, persistenceEnabled = false)
        val evaluator = DesktopConstraintEvaluator(config)
        assertNotNull(evaluator)
    }
}
