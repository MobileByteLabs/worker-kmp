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

import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.sync.DefaultUniqueWorkObserver
import io.github.mobilebytelabs.worker.sync.UniqueWorkObserver
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module binding [UniqueWorkObserver].
 *
 * Requires `single<WorkManager> { ... }` to be bound (typically via codegen-emitted
 * `installWorkerKmp{Platform}` from cmp-worker-app-plugin).
 *
 * Include in your Koin app graph alongside `WorkerKmpAuto.install()`:
 * ```kotlin
 * startKoin {
 *     modules(appKoinModules() + SyncObserverKoinModule)
 * }
 * WorkerKmpAuto.install()
 * ```
 */
public val SyncObserverKoinModule: Module = module {
    single<UniqueWorkObserver> { DefaultUniqueWorkObserver(workManager = get<WorkManager>()) }
}
