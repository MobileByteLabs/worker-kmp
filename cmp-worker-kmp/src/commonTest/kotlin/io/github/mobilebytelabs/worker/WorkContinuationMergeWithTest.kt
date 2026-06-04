/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Closes the residual coverage gap on [DefaultWorkContinuation] — specifically the
 * `WorkData.mergeWith` early-return branch where the receiver itself is `WorkData.EMPTY`
 * and the argument is non-empty. The main chain tests in [WorkContinuationTest] don't
 * reach that path because they always seed the first step's input data.
 */
class WorkContinuationMergeWithTest {

    private val wm = TestWorkManager()

    @Test
    fun mergeWith_receiverEmpty_returnsOther() {
        val empty = WorkData.EMPTY
        val other = workDataOf("k" to "v")
        val merged = empty.mergeWith(other)
        assertEquals("v", merged.getString("k"))
    }

    @Test
    fun mergeWith_otherEmpty_returnsReceiver() {
        val receiver = workDataOf("k" to "v")
        val merged = receiver.mergeWith(WorkData.EMPTY)
        assertEquals("v", merged.getString("k"))
    }

    @Test
    fun mergeWith_bothEmpty_returnsEmpty() {
        val merged = WorkData.EMPTY.mergeWith(WorkData.EMPTY)
        assertEquals(WorkData.EMPTY, merged)
    }

    private fun CoroutineScope.simulateSequentialSuccess(vararg ids: kotlin.uuid.Uuid) = launch {
        for (id in ids) {
            while (wm.getWorkInfoById(id) == null) delay(1)
            wm.simulateSuccess(id, WorkData.EMPTY)
        }
    }

    @Test
    fun continuation_firstStepEmptyOutput_secondStepInputUnchanged() = runTest {
        // First step emits WorkData.EMPTY → `carried` stays EMPTY → `executeStep`
        // for second step takes the `req` unchanged branch (the L111 `else req`).
        val a = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag("a").build()
        val b = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker")
            .setInputData(workDataOf("preserve" to "me"))
            .addTag("b")
            .build()

        simulateSequentialSuccess(a.id, b.id)

        wm.beginWith(a).then(b).enqueue()

        val bRequest = wm.enqueuedRequests.filterIsInstance<OneTimeWorkRequest>().first { it.id == b.id }
        // EMPTY-from-A means B's original `preserve=me` survives untouched.
        assertEquals("me", bRequest.inputData.getString("preserve"))
    }
}
