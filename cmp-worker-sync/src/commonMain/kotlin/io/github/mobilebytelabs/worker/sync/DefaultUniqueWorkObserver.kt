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

import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Default [UniqueWorkObserver] backed by [WorkManager.getWorkInfosForUniqueWorkFlow].
 *
 * No per-platform implementation needed — the underlying [WorkManager] surface is single-API.
 * This is the proof that worker-kmp's single-API claim extends cleanly when the runtime
 * primitive is single-API (closes audit gap G4 + AC-9 of the single-api-completion epic).
 *
 * `isRunning` collapses the per-state-list to a Boolean: `true` if ANY entry's state is
 * non-terminal (i.e., not SUCCEEDED / FAILED / CANCELLED).
 */
public class DefaultUniqueWorkObserver(private val workManager: WorkManager) : UniqueWorkObserver {

    override fun isRunning(uniqueWorkName: String): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)
            .map { infos -> infos.any { !it.state.isFinished } }

    override fun work(uniqueWorkName: String): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)
}
