package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Exercises [StoreRefreshScheduler] end-to-end against a recording fake [WorkManager].
 *
 * Covers:
 *  1. [StoreRefreshScheduler.schedulePeriodicRefresh] enqueues a [PeriodicWorkRequest]
 *     with the correct repeat interval, tag, and `enqueueUniquePeriodicWork(name, policy, req)`
 *     call.
 *  2. The tag carried on the request is `store5-refresh:<uniqueWorkName>` (the public
 *     observability contract).
 *  3. The default existing-policy is [ExistingPeriodicWorkPolicy.KEEP]; consumers can
 *     pass `REPLACE` to override.
 *  4. The default constraints are [Constraints.NONE]; passing a custom Constraints
 *     threads it onto the request.
 *  5. [StoreRefreshScheduler.cancelRefresh] forwards to `cancelAllWorkByTag(refreshTag)`.
 *  6. [StoreRefreshScheduler.observeRefreshes] is wired to `getWorkInfosByTag(refreshTag)`.
 *  7. Companion [StoreRefreshScheduler.TAG_PREFIX] is the stable observability constant.
 */
@OptIn(ExperimentalUuidApi::class)
class StoreRefreshSchedulerTest {

    @Test
    fun schedulePeriodicRefresh_enqueuesPeriodicRequest_withCorrectInterval() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        scheduler.schedulePeriodicRefresh<TestRefreshWorker>(
            interval = 6.hours,
            uniqueWorkName = "user-profile-sync",
        )

        assertEquals(1, wm.enqueuedPeriodics.size)
        val (uniqueName, policy, request) = wm.enqueuedPeriodics.single()
        assertEquals("user-profile-sync", uniqueName)
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, policy)
        assertEquals(6.hours, request.repeatInterval)
        assertEquals("TestRefreshWorker", request.workerClass)
    }

    @Test
    fun schedulePeriodicRefresh_tagsRequestWith_store5RefreshPrefix() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        scheduler.schedulePeriodicRefresh<TestRefreshWorker>(
            interval = 6.hours,
            uniqueWorkName = "currency-rates",
        )

        val (_, _, request) = wm.enqueuedPeriodics.single()
        assertTrue("store5-refresh:currency-rates" in request.tags)
    }

    @Test
    fun schedulePeriodicRefresh_passesCustomExistingPolicy_replace() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        scheduler.schedulePeriodicRefresh<TestRefreshWorker>(
            interval = 6.hours,
            uniqueWorkName = "currency-rates",
            existingPolicy = ExistingPeriodicWorkPolicy.REPLACE,
        )

        val (_, policy, _) = wm.enqueuedPeriodics.single()
        assertEquals(ExistingPeriodicWorkPolicy.REPLACE, policy)
    }

    @Test
    fun schedulePeriodicRefresh_defaultConstraints_isConstraintsNone() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        scheduler.schedulePeriodicRefresh<TestRefreshWorker>(
            interval = 6.hours,
            uniqueWorkName = "currency-rates",
        )

        val (_, _, request) = wm.enqueuedPeriodics.single()
        // Default Constraints.NONE means no network requirement
        assertEquals(NetworkType.NOT_REQUIRED, request.constraints.requiredNetworkType)
    }

    @Test
    fun schedulePeriodicRefresh_customConstraints_threadedToRequest() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)
        val customConstraints = Constraints { setRequiredNetworkType(NetworkType.UNMETERED) }

        scheduler.schedulePeriodicRefresh<TestRefreshWorker>(
            interval = 6.hours,
            uniqueWorkName = "currency-rates",
            constraints = customConstraints,
        )

        val (_, _, request) = wm.enqueuedPeriodics.single()
        assertEquals(NetworkType.UNMETERED, request.constraints.requiredNetworkType)
    }

    @Test
    fun cancelRefresh_cancelsByTaggedName() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        scheduler.cancelRefresh("currency-rates")

        assertTrue("store5-refresh:currency-rates" in wm.cancelledTags)
    }

    @Test
    fun observeRefreshes_subscribesToTaggedFlow() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        // Seed the fake with a WorkInfo tagged for `currency-rates`
        val id = Uuid.random()
        wm.seedWorkInfo(
            WorkInfo(
                id = id,
                state = WorkInfo.State.RUNNING,
                tags = setOf("store5-refresh:currency-rates"),
            ),
        )

        val observed = scheduler.observeRefreshes("currency-rates").first()
        assertEquals(1, observed.size)
        assertEquals(id, observed.single().id)
    }

    @Test
    fun observeRefreshes_emitsEmptyForUnknownName() = runTest {
        val wm = RecordingWorkManager()
        val scheduler = StoreRefreshScheduler(wm)

        val observed = scheduler.observeRefreshes("nothing-here").first()
        assertTrue(observed.isEmpty())
    }

    @Test
    fun tagPrefix_isStableConstant() {
        assertEquals("store5-refresh:", StoreRefreshScheduler.TAG_PREFIX)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test worker — concrete CoroutineWorker subclass for the reified generic.
// ──────────────────────────────────────────────────────────────────────────────

private class TestRefreshWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}

// ──────────────────────────────────────────────────────────────────────────────
// Recording fake WorkManager (this test module doesn't depend on cmp-worker-scheduler's
// FakeWorkManager — keep self-contained).
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalUuidApi::class)
private class RecordingWorkManager : WorkManager {
    data class EnqueuedPeriodic(
        val uniqueName: String,
        val policy: ExistingPeriodicWorkPolicy,
        val request: PeriodicWorkRequest,
    )

    private val _enqueuedOneTime = mutableListOf<OneTimeWorkRequest>()
    private val _enqueuedPeriodics = mutableListOf<EnqueuedPeriodic>()
    private val _cancelledIds = mutableSetOf<Uuid>()
    private val _cancelledTags = mutableSetOf<String>()
    private val state = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    val enqueuedOneTime: List<OneTimeWorkRequest> get() = _enqueuedOneTime.toList()
    val enqueuedPeriodics: List<EnqueuedPeriodic> get() = _enqueuedPeriodics.toList()
    val cancelledTags: Set<String> get() = _cancelledTags.toSet()

    fun seedWorkInfo(info: WorkInfo) {
        state.value = state.value + (info.id to info)
    }

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        _enqueuedOneTime += request
        state.value = state.value + (request.id to WorkInfo(request.id, WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        _enqueuedPeriodics += EnqueuedPeriodic(uniqueWorkName, existingPeriodicWorkPolicy, request)
        state.value = state.value + (request.id to WorkInfo(request.id, WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun cancelWorkById(id: Uuid) {
        _cancelledIds += id
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        _cancelledTags += tag
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> =
        state.map { it.values.filter { info -> tag in info.tags } }

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = state.value[id]
}
