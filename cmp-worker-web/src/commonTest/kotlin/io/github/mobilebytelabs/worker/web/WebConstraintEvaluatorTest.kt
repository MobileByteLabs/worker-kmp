package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.Constraints
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebConstraintEvaluatorTest {

    @Test
    fun evaluator_alwaysTrue_returnsTrue() = runTest {
        val evaluator = WebConstraintEvaluator { true }
        assertTrue(evaluator.evaluate(Constraints.NONE))
    }

    @Test
    fun evaluator_alwaysFalse_returnsFalse() = runTest {
        val evaluator = WebConstraintEvaluator { false }
        assertFalse(evaluator.evaluate(Constraints.NONE))
    }

    @Test
    fun evaluator_lambdaChecksConstraints_worksForBatteryNotLow() = runTest {
        val evaluator = WebConstraintEvaluator { constraints ->
            !constraints.requiresBatteryNotLow
        }
        val constraintsWithBattery = Constraints { setRequiresBatteryNotLow(true) }
        assertFalse(evaluator.evaluate(constraintsWithBattery))
        assertTrue(evaluator.evaluate(Constraints.NONE))
    }

    @Test
    fun evaluator_stateful_transitionsOnCall() = runTest {
        var callCount = 0
        val evaluator = WebConstraintEvaluator { ++callCount >= 2 }
        assertFalse(evaluator.evaluate(Constraints.NONE))
        assertTrue(evaluator.evaluate(Constraints.NONE))
    }
}
