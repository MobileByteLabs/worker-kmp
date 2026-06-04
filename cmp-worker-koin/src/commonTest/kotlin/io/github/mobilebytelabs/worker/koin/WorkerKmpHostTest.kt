/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.koin

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkerKmpHostTest {

    @BeforeTest
    fun resetState() {
        WorkerKmpHost.resetForTesting()
    }

    @AfterTest
    fun cleanup() {
        WorkerKmpHost.resetForTesting()
    }

    @Test
    fun initialize_defaultConfig_succeeds() {
        WorkerKmpHost.initialize()
        // Idempotent — second call is a no-op (verified indirectly via no crash + no exception).
        WorkerKmpHost.initialize()
    }

    @Test
    fun initialize_customConfig_acceptedWithoutError() {
        val config = WorkerKmpHostConfig(logTag = "test.worker")
        WorkerKmpHost.initialize(config)
    }

    @Test
    fun config_hasDefaultValues() {
        val config = WorkerKmpHostConfig()
        assertNull(config.koinScopeQualifier)
        assertEquals("worker-kmp.host", config.logTag)
    }

    @Test
    fun config_copy_preservesOtherFields() {
        val original = WorkerKmpHostConfig()
        val copied = original.copy(logTag = "custom-tag")
        assertEquals("custom-tag", copied.logTag)
        assertNull(copied.koinScopeQualifier)
    }

    @Test
    fun config_equals_byValue() {
        assertEquals(WorkerKmpHostConfig(), WorkerKmpHostConfig())
        assertEquals(
            WorkerKmpHostConfig(logTag = "x"),
            WorkerKmpHostConfig(logTag = "x"),
        )
    }
}
