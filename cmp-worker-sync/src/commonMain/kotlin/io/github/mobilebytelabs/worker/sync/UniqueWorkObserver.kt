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
import kotlinx.coroutines.flow.Flow

/**
 * Observable view over a [io.github.mobilebytelabs.worker.WorkManager]'s unique-work state.
 *
 * Closes audit gap G4 from the worker-kmp-single-api-completion epic — consumers no longer
 * need to hand-roll per-platform `SyncManager` files; they inject a [UniqueWorkObserver] from
 * Koin and consume `isRunning(name).collect { ... }` from any commonMain composable or
 * ViewModel.
 *
 * Use [isRunning] for the single binary "is any work under this unique name currently
 * enqueued or running" signal (the NiA-style sync indicator pattern). Use [work] for the
 * full list of [WorkInfo] entries (e.g., to inspect retry counts, output data, attempt
 * timestamps).
 *
 * Default implementation: [DefaultUniqueWorkObserver]. Koin binding shipped via
 * [io.github.mobilebytelabs.worker.sync.di.SyncObserverKoinModule].
 */
public interface UniqueWorkObserver {
    /** Emits `true` while any [WorkInfo] under [uniqueWorkName] is in a non-terminal state. */
    public fun isRunning(uniqueWorkName: String): Flow<Boolean>

    /** Reactive list of every [WorkInfo] enqueued under [uniqueWorkName]. */
    public fun work(uniqueWorkName: String): Flow<List<WorkInfo>>
}
