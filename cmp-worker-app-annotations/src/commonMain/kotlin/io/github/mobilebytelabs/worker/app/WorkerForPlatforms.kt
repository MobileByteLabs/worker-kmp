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

/**
 * Per-worker platform filter — opts a worker class into a SUBSET of generated
 * `installWorkerKmp{Platform}()` files.
 *
 * Some workers are inherently platform-specific — e.g. a `WebPushSubscriptionWorker` only
 * makes sense on `wasmJsMain`; a `KeystoreRotationWorker` only on Android. Annotating the
 * worker class with `@WorkerForPlatforms([Platform.Web])` causes the codegen to emit the
 * `register<WebPushSubscriptionWorker>` line ONLY in the Web platform-init file. Other
 * platforms' generated init files skip this worker.
 *
 * Default behavior (annotation absent) — worker is registered on all 4 platforms.
 *
 * The worker class must still compile under all targeted platforms (consumer responsibility).
 * The filter is for SKIPPING the register line, not for hiding the class.
 *
 * Consumer usage:
 *
 * ```kotlin
 * @WorkerForPlatforms([Platform.Web])
 * public class WebPushSubscriptionWorker(ctx: WorkerContext) : CoroutineWorker(ctx) {
 *     override suspend fun doWork(): WorkResult = WorkResult.success()
 * }
 *
 * @WorkerKmpWorkers(workers = [WebPushSubscriptionWorker::class, DataSyncWorker::class])
 * public fun workerDeclarations() = Unit
 * ```
 *
 * Generated Web init: `register<WebPushSubscriptionWorker>` + `register<DataSyncWorker>`.
 * Generated Android/iOS/Desktop inits: `register<DataSyncWorker>` only.
 *
 * @property platforms platforms on which to register this worker.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class WorkerForPlatforms(val platforms: Array<Platform>)
