package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.OutOfQuotaPolicy
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Tests for [DefaultWorkScheduler] covering the v3.1.1 typed reified worker API +
 * builder lambda overrides. Backed by [FakeWorkManager] (records every enqueue) so
 * each test inspects the `OneTimeWorkRequest` / `PeriodicWorkRequest` actually built.
 */
@OptIn(ExperimentalTime::class)
class DefaultWorkSchedulerTest {

    private fun newScheduler(
        workManager: FakeWorkManager = FakeWorkManager(),
    ): Pair<FakeWorkManager, DefaultWorkScheduler> {
        val scheduler = DefaultWorkScheduler(
            workManager = workManager,
            persister = SyncStatePersister(),
            scope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()),
        )
        return workManager to scheduler
    }

    // ─── enqueueDataSync ───────────────────────────────────────────────────────

    @Test
    fun enqueueDataSync_buildsOneTimeWorkRequest_withTypedWorkerClass() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>(
            payload = workDataOf("k" to "v"),
        )
        assertNotNull(handle)
        assertEquals(SYNC_WORK_NAME, handle.uniqueName)
        assertEquals(1, wm.enqueuedRequests.size)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        // Worker class name is reified from <FakeSyncWorker>
        assertEquals("FakeSyncWorker", request.workerClass)
        // Payload propagates
        assertEquals("v", request.inputData.getString("k"))
        // Default tag is SYNC_WORK_NAME
        assertTrue(SYNC_WORK_NAME in request.tags)
    }

    @Test
    fun enqueueDataSync_foreground_setsExpeditedPolicy() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.enqueueDataSync<FakeSyncWorker>(mode = WorkMode.Foreground)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        assertEquals(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, request.expeditedPolicy)
    }

    @Test
    fun enqueueDataSync_background_doesNotSetExpedited() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.enqueueDataSync<FakeSyncWorker>(mode = WorkMode.Background)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        assertEquals(null, request.expeditedPolicy)
    }

    @Test
    fun enqueueDataSync_builderLambda_overridesDefaults_andAppliesAfter() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.enqueueDataSync<FakeSyncWorker> {
            addTag("consumer-custom-tag")
        }
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        // Both library default + consumer override tags are present
        assertTrue(SYNC_WORK_NAME in request.tags)
        assertTrue("consumer-custom-tag" in request.tags)
    }

    // ─── scheduleDailyDataSync ────────────────────────────────────────────────

    @Test
    fun scheduleDailyDataSync_enqueuesPeriodicWithKeepPolicy_24hInterval() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.scheduleDailyDataSync<FakeSyncWorker>(
            timeOfDay = LocalTime(hour = 9, minute = 0),
            timeZone = TimeZone.UTC,
        )
        assertEquals(1, wm.enqueuedRequests.size)
        val request = wm.enqueuedRequests.single() as PeriodicWorkRequest
        assertEquals("FakeSyncWorker", request.workerClass)
        assertEquals(24.hours, request.repeatInterval)
        assertTrue(DAILY_SYNC_WORK_NAME in request.tags)
        assertEquals(
            ExistingPeriodicWorkPolicy.KEEP,
            wm.periodicPolicyByUniqueName[DAILY_SYNC_WORK_NAME],
        )
    }

    @Test
    fun scheduleDailyDataSync_initialDelay_isPositiveAndUnder24Hours() = runTest {
        val (wm, scheduler) = newScheduler()
        // Schedule for some fixed time; verify the impl computes a valid initialDelay
        // bounded in (0, 24h]. The exact value depends on current wall-clock at runtime.
        scheduler.scheduleDailyDataSync<FakeSyncWorker>(
            timeOfDay = LocalTime(hour = 9, minute = 0),
            timeZone = TimeZone.UTC,
        )
        val request = wm.enqueuedRequests.single() as PeriodicWorkRequest
        assertTrue(request.initialDelay >= kotlin.time.Duration.ZERO)
        assertTrue(request.initialDelay <= 24.hours)
    }

    // ─── schedulePeriodicDataSync ─────────────────────────────────────────────

    @Test
    fun schedulePeriodicDataSync_clampsSubMinimumIntervalTo15min() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.schedulePeriodicDataSync<FakeSyncWorker>(interval = 5.minutes)
        val request = wm.enqueuedRequests.single() as PeriodicWorkRequest
        assertEquals(15.minutes, request.repeatInterval)
    }

    @Test
    fun schedulePeriodicDataSync_respectsIntervalAboveMinimum() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.schedulePeriodicDataSync<FakeSyncWorker>(interval = 6.hours)
        val request = wm.enqueuedRequests.single() as PeriodicWorkRequest
        assertEquals(6.hours, request.repeatInterval)
        assertTrue(PERIODIC_SYNC_WORK_NAME in request.tags)
    }

    // ─── scheduleDataSyncAt + scheduleDataSyncAtExact ─────────────────────────

    @Test
    fun scheduleDataSyncAt_setsInitialDelay_andEmbedsInstantInUniqueName() = runTest {
        val (wm, scheduler) = newScheduler()
        val futureInstant = Clock.System.now() + 10.minutes
        val handle = scheduler.scheduleDataSyncAt<FakeSyncWorker>(
            instant = futureInstant,
            payload = workDataOf("loan.id" to "L42"),
        )
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        assertEquals("L42", request.inputData.getString("loan.id"))
        assertEquals("data-sync-at-${futureInstant.toEpochMilliseconds()}", handle.uniqueName)
        assertTrue(request.initialDelay >= 9.minutes && request.initialDelay <= 11.minutes)
    }

    @Test
    fun scheduleDataSyncAtExact_commonPath_delegatesToScheduleDataSyncAt() = runTest {
        val (wm, scheduler) = newScheduler()
        val instant = Clock.System.now() + 5.minutes
        scheduler.scheduleDataSyncAtExact<FakeSyncWorker>(instant)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        // Same shape as scheduleDataSyncAt — common platform uses flex-window
        assertEquals("FakeSyncWorker", request.workerClass)
        assertTrue(request.initialDelay > 0.minutes)
    }

    @Test
    fun scheduleDataSyncAt_pastInstant_clampsDelayToZero() = runTest {
        val (wm, scheduler) = newScheduler()
        val pastInstant = Clock.System.now() - 1.hours
        scheduler.scheduleDataSyncAt<FakeSyncWorker>(pastInstant)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        assertEquals(kotlin.time.Duration.ZERO, request.initialDelay)
    }

    // ─── Multi-worker isolation ───────────────────────────────────────────────

    @Test
    fun multipleWorkerTypes_sameSchedulerInstance_threadCorrectClassName() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.enqueueDataSync<FakeSyncWorker>()
        scheduler.enqueueDataSync<FakeAnalyticsWorker>()
        assertEquals(2, wm.enqueuedRequests.size)
        assertEquals("FakeSyncWorker", (wm.enqueuedRequests[0] as OneTimeWorkRequest).workerClass)
        assertEquals("FakeAnalyticsWorker", (wm.enqueuedRequests[1] as OneTimeWorkRequest).workerClass)
    }

    // ─── observeWork + cancelWork ─────────────────────────────────────────────

    @Test
    fun observeWork_emitsPending_thenSucceeded_onSimulatedTransition() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>()
        // Initial state — Pending (mapped from ENQUEUED)
        val initial = scheduler.observeWork(SYNC_WORK_NAME).first()
        assertEquals(WorkStatus.Pending, initial)
        // Flip to SUCCEEDED via the fake
        wm.simulateSuccess(handle.id)
        val terminal = scheduler.observeWork(SYNC_WORK_NAME).first()
        assertEquals(WorkStatus.Succeeded, terminal)
    }

    @Test
    fun cancelWork_callsCancelAllWorkByTag_onFake() = runTest {
        val (wm, scheduler) = newScheduler()
        scheduler.enqueueDataSync<FakeSyncWorker>()
        scheduler.cancelWork(SYNC_WORK_NAME)
        assertTrue(SYNC_WORK_NAME in wm.cancelledTags)
    }

    @Test
    fun observeWork_emitsRunning_onSimulatedRunning() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>()
        wm.simulateRunning(handle.id)
        assertEquals(WorkStatus.Running, scheduler.observeWork(SYNC_WORK_NAME).first())
    }

    @Test
    fun observeWork_emitsFailed_onSimulatedFailure() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>()
        wm.simulateFailure(handle.id)
        assertEquals(WorkStatus.Failed, scheduler.observeWork(SYNC_WORK_NAME).first())
    }

    @Test
    fun observeWork_emitsCancelled_onCancelWork() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>()
        wm.cancelWorkById(handle.id)
        assertEquals(WorkStatus.Cancelled, scheduler.observeWork(SYNC_WORK_NAME).first())
    }

    @Test
    fun scheduleDailyDataSync_nextOccurrenceOf_initialDelayWithinOnePeriod() = runTest {
        val (wm, scheduler) = newScheduler()
        // Schedule for 23:59 UTC to cover the if-branch (target still in the future most of the day)
        // and for 00:01 UTC to cover the else-branch (target has already passed today).
        // Both should produce a delay in [0, 24h].
        for (time in listOf(LocalTime(23, 59), LocalTime(0, 1))) {
            wm.clearRequests()
            scheduler.scheduleDailyDataSync<FakeSyncWorker>(
                timeOfDay = time,
                timeZone = TimeZone.UTC,
            )
            val delay = (wm.enqueuedRequests.single() as PeriodicWorkRequest).initialDelay
            assertTrue(
                delay >= 0.minutes && delay <= 24.hours,
                "Expected delay in [0, 24h] for $time but was $delay",
            )
        }
    }

    @Test
    fun observeWork_emitsPending_whenNothingEnqueued() = runTest {
        val (_, scheduler) = newScheduler()
        // No work tagged "nonexistent" → empty infos list → null state → Pending
        assertEquals(WorkStatus.Pending, scheduler.observeWork("nonexistent").first())
    }

    @Test
    fun observeWork_emitsPending_onSimulatedBlocked() = runTest {
        val (wm, scheduler) = newScheduler()
        val handle = scheduler.enqueueDataSync<FakeSyncWorker>()
        wm.simulateBlocked(handle.id)
        assertEquals(WorkStatus.Pending, scheduler.observeWork(SYNC_WORK_NAME).first())
    }

    @Test
    fun workHandle_secondaryConstructor_generatesRandomId() {
        val h1 = WorkHandle(uniqueName = "test-name")
        val h2 = WorkHandle(uniqueName = "test-name")
        assertEquals("test-name", h1.uniqueName)
        assertTrue(h1.id != h2.id, "Each secondary-constructor call should produce a unique id")
    }
}
