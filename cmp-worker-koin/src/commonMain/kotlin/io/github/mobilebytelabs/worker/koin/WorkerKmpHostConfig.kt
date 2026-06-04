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

import org.koin.core.qualifier.Qualifier

/**
 * Configuration for [WorkerKmpHost.initialize].
 *
 * Delivered to codegen-emitted `installWorkerKmp{Platform}` via Koin (D22) — consumer binds
 * `single { WorkerKmpHostConfig(...) }` in their Koin module if they want to override
 * defaults; otherwise codegen falls back to this data class's default values via
 * `getKoin().getOrNull<WorkerKmpHostConfig>() ?: WorkerKmpHostConfig()`.
 *
 * Minimal 2-field surface per H6 audit finding — no `autoFirstSync` / `firstSyncUniqueName`
 * (those concerns moved to consumer code; library does not enqueue work as part of init).
 *
 * @property koinScopeQualifier optional Koin scope qualifier — set if the consumer uses a
 *   scoped Koin instance for worker dependencies. `null` (default) means workers resolve
 *   from the global Koin scope.
 * @property logTag prefix used by worker-kmp's internal Kermit logger for host-side messages.
 *   Defaults to `"worker-kmp.host"`.
 */
public data class WorkerKmpHostConfig(
    val koinScopeQualifier: Qualifier? = null,
    val logTag: String = "worker-kmp.host",
)
