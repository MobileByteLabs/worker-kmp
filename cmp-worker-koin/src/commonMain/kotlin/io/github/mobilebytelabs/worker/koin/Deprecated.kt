/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
@file:Suppress("UNUSED_PARAMETER", "unused")

package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry

/**
 * REMOVED in v4.0.0. Use [WorkerKmpAuto.install()] (commonMain) instead, after declaring
 * workers with `@WorkerKmpWorkers([…])`.
 *
 * This tombstone exists so the Kotlin IDE can offer a quick-fix migration via
 * [Deprecated.replaceWith]. Calling this function at any source location produces an
 * error-level compile diagnostic.
 *
 * Migration:
 * - **Old**: `modules(workKoinModule(WorkerConfig(), workerRegistry { register<W>{ ctx -> W(ctx) } }, androidWorkManagerFactory(this)))`
 * - **New**: `@WorkerKmpWorkers([W::class]) fun workerDeclarations() = Unit` + `modules(appKoinModules())` + `WorkerKmpAuto.install()` after `startKoin { androidContext(this@App); … }`.
 *
 * Full migration guide:
 * https://github.com/MobileByteLabs/worker-kmp/wiki/single-api-guide
 */
@Deprecated(
    message = "Removed in v4.0.0 — use @WorkerKmpWorkers([…]) + WorkerKmpAuto.install() instead. " +
        "See https://github.com/MobileByteLabs/worker-kmp/wiki/single-api-guide.",
    replaceWith = ReplaceWith(
        expression = "WorkerKmpAuto.install()",
        imports = ["io.github.mobilebytelabs.worker.app.WorkerKmpAuto"],
    ),
    level = DeprecationLevel.ERROR,
)
public fun workKoinModule(config: WorkerConfig, workers: WorkerRegistry, factory: WorkManagerFactory): Nothing =
    error("workKoinModule was removed in v4.0.0 — see migration guide.")
