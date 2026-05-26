package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.Constraints

internal fun interface WebConstraintEvaluator {
    suspend fun evaluate(constraints: Constraints): Boolean
}

internal expect fun defaultConstraintEvaluator(): WebConstraintEvaluator
