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

import co.touchlab.kermit.Logger

/**
 * Single host-app entry-point for worker-kmp initialization.
 *
 * Invoked by codegen-emitted `installWorkerKmp{Platform}()` files (from `cmp-worker-app-plugin`)
 * — consumer's code never calls this directly; they call `WorkerKmpAuto.install()` instead,
 * which is the codegen-emitted commonMain shim that dispatches to the per-platform install
 * function which in turn calls this.
 *
 * **NON-SUSPEND** — pure setup state work; safe to call from `Application.onCreate` or any
 * other non-coroutine context. No I/O, no work enqueuing, no thread-blocking operations.
 * Closes audit gap H5 (suspend `initialize` from main thread → ANR risk).
 *
 * **Idempotent** — multiple calls after the first are no-ops. Survives Android
 * `Application.onCreate` re-invocation patterns + iOS app-launch retries.
 *
 * **First-sync is consumer's responsibility** — this function does NOT enqueue any work.
 * After `WorkerKmpAuto.install()` returns, the consumer's `App.onCreate` may explicitly call
 * `get<WorkManager>().enqueueUniqueWork(...)` or similar to fire a first sync. Closes audit
 * gap H6 (auto-first-sync duplicates consumer responsibility) + H3 (no cross-epic dep on
 * worker-kmp-scheduler-api's `WorkScheduler`).
 *
 * @see WorkerKmpHostConfig for configuration options (delivered via Koin binding per D22).
 */
public object WorkerKmpHost {

    // Single-threaded init context (Application.onCreate / iOS main / Desktop main / Web main)
    // — no concurrent access expected. First-call-wins idempotence; subsequent calls no-op.
    private var initialized: Boolean = false

    /**
     * Initialize worker-kmp host state.
     *
     * Idempotent — first call wins; subsequent calls return immediately without side effects.
     * Latency target: <50ms (AC-57) — pure setup, no I/O.
     *
     * @param config host configuration; defaults to [WorkerKmpHostConfig] sensible defaults.
     *   Codegen-emitted `installWorkerKmp{Platform}` resolves this via
     *   `getKoin().getOrNull<WorkerKmpHostConfig>() ?: WorkerKmpHostConfig()` (D22).
     */
    public fun initialize(config: WorkerKmpHostConfig = WorkerKmpHostConfig()) {
        if (initialized) return
        initialized = true
        Logger.withTag(config.logTag).i { "WorkerKmpHost initialized" }
    }

    /** Test-only — exposes the idempotence flag for `WorkerKmpHostIdempotenceTest`. */
    internal fun resetForTesting() {
        initialized = false
    }
}
