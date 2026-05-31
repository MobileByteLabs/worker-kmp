package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister

/**
 * Concrete [AbstractDataSyncWorker] subclass used at the test call site to satisfy
 * the `KClass<W : AbstractDataSyncWorker>` bound on every schedule method. Never
 * instantiated — the scheduler only reads `FakeSyncWorker::class.simpleName` to
 * thread the worker name through to the WorkRequest.
 */
internal class FakeSyncWorker(ctx: WorkerContext) :
    AbstractDataSyncWorker(ctx, syncables = emptyList(), persister = SyncStatePersister())

/** A second concrete subclass for multi-worker tests. */
internal class FakeAnalyticsWorker(ctx: WorkerContext) :
    AbstractDataSyncWorker(ctx, syncables = emptyList(), persister = SyncStatePersister())
