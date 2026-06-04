/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.sync

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.ExistingWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class DefaultUniqueWorkObserverTest {

    @Test
    fun isRunning_emptyList_returnsFalse() = runTest {
        val observer = DefaultUniqueWorkObserver(FakeWorkManager(emptyList()))
        assertFalse(observer.isRunning("any-name").first())
    }

    @Test
    fun isRunning_singleEnqueued_returnsTrue() = runTest {
        val infos = listOf(workInfoWith(WorkInfo.State.ENQUEUED))
        val observer = DefaultUniqueWorkObserver(FakeWorkManager(infos))
        assertTrue(observer.isRunning("name").first())
    }

    @Test
    fun isRunning_singleRunning_returnsTrue() = runTest {
        val infos = listOf(workInfoWith(WorkInfo.State.RUNNING))
        val observer = DefaultUniqueWorkObserver(FakeWorkManager(infos))
        assertTrue(observer.isRunning("name").first())
    }

    @Test
    fun isRunning_terminalStates_returnFalse() = runTest {
        for (state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)) {
            val infos = listOf(workInfoWith(state))
            val observer = DefaultUniqueWorkObserver(FakeWorkManager(infos))
            assertFalse(observer.isRunning("name").first(), "state=$state should be terminal")
        }
    }

    @Test
    fun isRunning_mixedTerminalAndRunning_returnsTrue() = runTest {
        val infos = listOf(
            workInfoWith(WorkInfo.State.SUCCEEDED),
            workInfoWith(WorkInfo.State.RUNNING),
        )
        val observer = DefaultUniqueWorkObserver(FakeWorkManager(infos))
        // Any non-terminal entry should flip isRunning to true.
        assertTrue(observer.isRunning("name").first())
    }

    @Test
    fun work_passesUnmodified() = runTest {
        val infos = listOf(
            workInfoWith(WorkInfo.State.ENQUEUED),
            workInfoWith(WorkInfo.State.SUCCEEDED),
        )
        val observer = DefaultUniqueWorkObserver(FakeWorkManager(infos))
        assertEquals(infos, observer.work("name").first())
    }

    private fun workInfoWith(state: WorkInfo.State): WorkInfo = WorkInfo(
        id = Uuid.random(),
        state = state,
        tags = emptySet(),
        outputData = io.github.mobilebytelabs.worker.WorkData.EMPTY,
        progress = io.github.mobilebytelabs.worker.WorkProgress(0),
        runAttemptCount = 0,
    )

    /** Bare-minimum WorkManager fake — only [getWorkInfosForUniqueWorkFlow] is used by the SUT. */
    private class FakeWorkManager(initial: List<WorkInfo>) : WorkManager {
        private val state = MutableStateFlow(initial)
        override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> = state
        override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = state

        // Unused — must be implemented for the interface contract.
        override suspend fun enqueue(request: OneTimeWorkRequest): Uuid = error("not used")
        override suspend fun enqueueUniquePeriodicWork(
            uniqueWorkName: String,
            existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
            request: PeriodicWorkRequest,
        ): Uuid = error("not used")
        override suspend fun enqueueUniqueWork(
            uniqueWorkName: String,
            existingWorkPolicy: ExistingWorkPolicy,
            request: OneTimeWorkRequest,
        ): Uuid = error("not used")
        override suspend fun cancelWorkById(id: Uuid) = error("not used")
        override suspend fun cancelAllWorkByTag(tag: String) = error("not used")
        override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = null
    }
}
