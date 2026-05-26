package io.github.mobilebytelabs.worker.web

internal actual fun defaultConstraintEvaluator(): WebConstraintEvaluator =
    WebConstraintEvaluator { true }
