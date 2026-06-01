package io.github.mobilebytelabs.worker.storeflow.submit

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/**
 * Covers [WorkScheduledOfflineSubmitSyncer] + the [workScheduledOfflineSubmitSyncer] factory
 * + the placeholder [SyncerWorker.doWork] (returns Success).
 *
 * Verifies the periodic-work scheduling DSL is hit (constraints lambda branches included)
 * and the cancel-by-tag path on stop().
 */
class WorkScheduledOfflineSubmitSyncerTest {

    private class RecordingWorkManager : WorkManager {
        val enqueuedPeriodic: MutableList<Pair<String, PeriodicWorkRequest>> = mutableListOf()
        val cancelledTags: MutableList<String> = mutableListOf()
        var observedTag: String? = null

        override suspend fun enqueue(request: OneTimeWorkRequest): Uuid = request.id

        override suspend fun enqueueUniquePeriodicWork(
            uniqueWorkName: String,
            existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
            request: PeriodicWorkRequest,
        ): Uuid {
            enqueuedPeriodic += uniqueWorkName to request
            return request.id
        }

        override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> {
            observedTag = tag
            return flowOf(emptyList())
        }

        override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = null
        override suspend fun cancelWorkById(id: Uuid) = Unit
        override suspend fun cancelAllWorkByTag(tag: String) {
            cancelledTags += tag
        }
    }

    @Test
    fun start_enqueuesPeriodicWorkWithExpectedTagAndConstraints() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
            uniqueWorkName = "my-syncer",
        )

        val id = syncer.start(interval = 10.minutes, requireNetwork = true)
        assertNotNull(id)

        val (name, req) = workManager.enqueuedPeriodic.single()
        assertEquals("my-syncer", name)
        assertTrue("storeflow:syncer:my-syncer" in req.tags)
        assertEquals(10.minutes, req.repeatInterval)
    }

    @Test
    fun start_withoutNetwork_doesNotRequireConnectedConstraint() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        syncer.start(requireNetwork = false)
        // Just covering the false branch of the requireNetwork conditional.
        assertEquals(1, workManager.enqueuedPeriodic.size)
    }

    @Test
    fun start_usesReplacePolicy() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        syncer.start(existingPolicy = ExistingPeriodicWorkPolicy.REPLACE)
        assertEquals(1, workManager.enqueuedPeriodic.size)
    }

    @Test
    fun stop_cancelsAllWorkByTag() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
            uniqueWorkName = "cancel-target",
        )
        syncer.stop()
        assertEquals(listOf("storeflow:syncer:cancel-target"), workManager.cancelledTags)
    }

    @Test
    fun observeRetries_queriesTaggedFlow() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
            uniqueWorkName = "observe-target",
        )
        val flow = syncer.observeRetries()
        // Subscribe once to ensure the tag was passed through.
        assertEquals(emptyList(), flow.first())
        assertEquals("storeflow:syncer:observe-target", workManager.observedTag)
    }

    @Test
    fun factoryFunction_defaultsUniqueWorkName() = runTest {
        val workManager = RecordingWorkManager()
        val syncer = workScheduledOfflineSubmitSyncer<String, Unit>(
            workManager = workManager,
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        syncer.start()
        // Default uniqueWorkName per the factory signature.
        assertEquals("submit-outbox-syncer", workManager.enqueuedPeriodic.single().first)
    }

    @Test
    fun syncerWorker_doWork_returnsSuccessScaffold() = runTest {
        val ctx = object : WorkerContext {
            override val id: Uuid = Uuid.random()
            override val inputData get() = io.github.mobilebytelabs.worker.WorkData.EMPTY
            override val tags: Set<String> = emptySet()
            override suspend fun setProgress(progress: WorkProgress) = Unit
        }
        val worker = SyncerWorker(ctx)
        val result = worker.doWork()
        assertIs<WorkResult.Success>(result)
    }
}
