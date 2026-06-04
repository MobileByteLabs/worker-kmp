/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.registry

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.TestWorkerContext
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Closes the residual coverage gap on [WorkerRegistry] —
 * - exception message rendering on [WorkerRegistryAlreadyLoadedException]
 * - registry built with multiple types + introspection via [registeredClassNames]
 * - the non-typed `register(className, factory)` overload (the inline-reified
 *   variant in [WorkerRegistryTest] only exercises the simpleName-derived path)
 */
private class W1(ctx: WorkerContext) : CoroutineWorker(ctx) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}

private class W2(ctx: WorkerContext) : CoroutineWorker(ctx) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}

class WorkerRegistryExtraTest {

    @Test
    fun alreadyLoadedException_messageContainsAttemptedRegistration() {
        val ex = WorkerRegistryAlreadyLoadedException("com.example.Late")
        assertTrue(ex.message!!.contains("com.example.Late"), "exception message must surface the bad registration")
    }

    @Test
    fun alreadyLoadedException_isIllegalStateException() {
        val ex = WorkerRegistryAlreadyLoadedException("X")
        assertTrue(ex is IllegalStateException)
    }

    @Test
    fun registry_multipleTypes_allDiscoverableByClassName() {
        val r = workerRegistry {
            register<W1> { ctx -> W1(ctx) }
            register<W2> { ctx -> W2(ctx) }
        }
        assertEquals(setOf("W1", "W2"), r.registeredClassNames())
        assertNotNull(r.create("W1", TestWorkerContext()))
        assertNotNull(r.create("W2", TestWorkerContext()))
    }

    @Test
    fun stringOverload_registersWithoutClassObject() {
        val r = workerRegistry { }
        r.register("ManuallyKeyed") { ctx -> W1(ctx) }
        val w = r.create("ManuallyKeyed", TestWorkerContext())
        assertNotNull(w)
        assertTrue(w is W1)
    }

    @Test
    fun registeredClassNames_returnsImmutableCopy() {
        val r = workerRegistry {
            register<W1> { ctx -> W1(ctx) }
        }
        val first = r.registeredClassNames()
        r.register("Other") { ctx -> W1(ctx) }
        val second = r.registeredClassNames()
        // First snapshot must NOT see "Other" — confirms .toSet() copy semantics.
        assertTrue("Other" !in first)
        assertTrue("Other" in second)
    }

    @Test
    fun lock_isIdempotent() {
        val r = workerRegistry { register<W1> { ctx -> W1(ctx) } }
        r.lock()
        r.lock() // second lock must be a no-op (must not throw)
    }
}
