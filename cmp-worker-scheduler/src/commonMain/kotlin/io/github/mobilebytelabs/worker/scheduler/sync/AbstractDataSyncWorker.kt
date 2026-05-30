package io.github.mobilebytelabs.worker.scheduler.sync

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Base class for cross-platform data-sync workers. Consumers extend + list their
 * Syncable repos in the constructor; doWork iterates them in parallel via awaitAll.
 *
 * Per D5/D8: constructor-injected `List<Syncable>` (NOT runtime getAll<Syncable>());
 * fan-out is visible in subclass's constructor signature.
 */
abstract class AbstractDataSyncWorker(
    ctx: WorkerContext,
    private val syncables: List<Syncable>,
    private val persister: SyncStatePersister,
) : CoroutineWorker(ctx),
    Synchronizer {

    private var workingVersions: ChangeListVersions = ChangeListVersions()

    override suspend fun getChangeListVersions() = workingVersions
    override suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions) {
        workingVersions = workingVersions.update()
    }

    override suspend fun doWork(): WorkResult {
        workingVersions = persister.read()
        return try {
            val results = coroutineScope {
                syncables.map { syncable ->
                    async { syncable.syncWith(this@AbstractDataSyncWorker, inputData) }
                }.awaitAll()
            }
            if (results.all { it }) {
                persister.write(workingVersions)
                WorkResult.success()
            } else {
                WorkResult.retry()
            }
        } catch (t: Throwable) {
            WorkResult.failure(t.message ?: t::class.simpleName ?: "sync failed")
        }
    }
}
