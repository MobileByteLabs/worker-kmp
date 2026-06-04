/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.worker.app

import kotlin.reflect.KClass

/**
 * Declares which `CoroutineWorker` subclasses participate in codegen-driven worker
 * registration.
 *
 * Closes audit gap G1 (per-platform factory selection) — the build-time KSP processor reads
 * `@WorkerKmpWorkers` annotation sites and emits per-platform `installWorkerKmp{Platform}()`
 * files containing the appropriate `workerRegistry { register<…> { … } }` block + the
 * matching `*WorkManagerFactory(...)` selection. Consumer's commonMain code never sees the
 * factory or the registry.
 *
 * **Independent of [WorkerKmpApp]** — apply to any commonMain top-level function (typically
 * a marker `public fun workerDeclarations() = Unit`). Consumers using full app codegen via
 * `@WorkerKmpApp` may also apply `@WorkerKmpWorkers`; consumers writing their own
 * `Application` class use only `@WorkerKmpWorkers` + call `WorkerKmpAuto.install()` from their
 * own `onCreate` (Shape 2 from the wiki guide).
 *
 * **Within-module aggregation only** (D23 per H2 audit finding) — multiple
 * `@WorkerKmpWorkers` annotation sites within the SAME module are aggregated by KSP into one
 * registry. Cross-module aggregation is NOT supported (`SOURCE` retention + KSP per-module
 * scope). Consumer's wiki guide directs them to place annotation site(s) in the module that
 * depends on every worker-owning module.
 *
 * **Optional** (D31 per M1 audit finding) — `@WorkerKmpApp` consumers without workers may
 * omit `@WorkerKmpWorkers`. Codegen emits an empty `workerRegistry { }` block.
 *
 * **Visibility constraint** (D18 per H7 audit finding) — every class listed in [workers] AND
 * each of its primary-constructor dep types MUST be `public`. KSP processor enforces this at
 * compile time with clear suggested-fix error messages.
 *
 * **Default-valued constructor params** (D29 per NG12) — primary-constructor params with
 * default values are SKIPPED from Koin autowiring; the generated `register<…>` block uses
 * the default value.
 *
 * Consumer usage (in commonMain):
 *
 * ```kotlin
 * @WorkerKmpWorkers(workers = [DataSyncWorker::class, NotificationWorker::class])
 * public fun workerDeclarations() = Unit
 * ```
 *
 * @property workers worker classes — each MUST extend `CoroutineWorker` with `WorkerContext`
 *   as first primary-constructor param + be `public`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class WorkerKmpWorkers(val workers: Array<KClass<*>>)
