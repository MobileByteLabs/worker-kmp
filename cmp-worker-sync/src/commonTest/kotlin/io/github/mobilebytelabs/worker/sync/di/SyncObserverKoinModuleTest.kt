/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.sync.di

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.ExistingWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.sync.DefaultUniqueWorkObserver
import io.github.mobilebytelabs.worker.sync.UniqueWorkObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class SyncObserverKoinModuleTest {

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun bindsUniqueWorkObserverAsSingle() {
        val koin = startKoin {
            modules(
                module { single<WorkManager> { StubWorkManager() } },
                SyncObserverKoinModule,
            )
        }.koin
        val observer = koin.get<UniqueWorkObserver>()
        assertNotNull(observer)
        assertTrue(observer is DefaultUniqueWorkObserver)
    }

    @Test
    fun singleInstance_returnedTwice_isSameInstance() {
        val koin = startKoin {
            modules(
                module { single<WorkManager> { StubWorkManager() } },
                SyncObserverKoinModule,
            )
        }.koin
        val first = koin.get<UniqueWorkObserver>()
        val second = koin.get<UniqueWorkObserver>()
        assertTrue(first === second, "single<> bindings must return the same instance across resolutions")
    }

    private class StubWorkManager : WorkManager {
        override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = flowOf(emptyList())
        override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> = flowOf(emptyList())
        override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = null
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
    }
}
