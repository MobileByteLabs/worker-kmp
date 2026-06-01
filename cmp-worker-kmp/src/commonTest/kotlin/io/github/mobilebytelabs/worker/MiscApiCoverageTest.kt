package io.github.mobilebytelabs.worker

import io.github.mobilebytelabs.worker.config.AndroidWorkerConfig
import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig
import io.github.mobilebytelabs.worker.config.DesktopWorkerConfig
import io.github.mobilebytelabs.worker.config.IosWorkerConfig
import io.github.mobilebytelabs.worker.config.LogLevel
import io.github.mobilebytelabs.worker.config.WebWorkerConfig
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.observer.LoggingWorkObserver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Catch-all coverage for the smaller commonMain types that weren't worth a dedicated
 * test file: [WorkResult] sealed subtypes, [WorkInfo] data class, [WorkEvent] variants,
 * exception types, enum entries, config defaults, [LoggingWorkObserver] dispatch.
 *
 * Per Phase 4 (kover-100-coverage) GOAL.md AC #10.
 */
class MiscApiCoverageTest {

    // --- WorkResult sealed subtypes --------------------------------------

    @Test
    fun workResult_success_defaultOutputDataIsEmpty() {
        val s = WorkResult.Success()
        assertEquals(WorkData.EMPTY, s.outputData)
    }

    @Test
    fun workResult_success_dataClassOps() {
        val payload = workDataOf("out" to "v")
        val s = WorkResult.Success(payload)
        assertEquals(payload, s.component1())
        val copied = s.copy(outputData = WorkData.EMPTY)
        assertNotEquals(s, copied)
        assertEquals(WorkData.EMPTY, copied.outputData)
        assertTrue("Success" in s.toString())
        assertEquals(s, WorkResult.Success(payload))
        assertEquals(s.hashCode(), WorkResult.Success(payload).hashCode())
    }

    @Test
    fun workResult_failure_defaultsAreEmpty() {
        val f = WorkResult.Failure()
        assertEquals("", f.message)
        assertEquals(WorkData.EMPTY, f.outputData)
    }

    @Test
    fun workResult_failure_dataClassOps() {
        val f = WorkResult.Failure(message = "boom", outputData = workDataOf("err" to "x"))
        assertEquals("boom", f.component1())
        assertEquals("x", f.component2().getString("err"))
        val copied = f.copy(message = "still bad")
        assertEquals("still bad", copied.message)
        assertNotEquals(f, copied)
        assertTrue("Failure" in f.toString())
        assertEquals(
            f.hashCode(),
            WorkResult.Failure(message = "boom", outputData = workDataOf("err" to "x")).hashCode(),
        )
    }

    @Test
    fun workResult_retry_defaultReason() {
        val r = WorkResult.Retry()
        assertEquals("", r.reason)
    }

    @Test
    fun workResult_retry_dataClassOps() {
        val r = WorkResult.Retry("rate limited")
        assertEquals("rate limited", r.component1())
        val copied = r.copy(reason = "transient")
        assertEquals("transient", copied.reason)
        assertNotEquals(r, copied)
        assertTrue("Retry" in r.toString())
        assertEquals(r, WorkResult.Retry("rate limited"))
        assertEquals(r.hashCode(), WorkResult.Retry("rate limited").hashCode())
    }

    @Test
    fun workResult_companionFactories_returnExpectedTypes() {
        assertTrue(WorkResult.success() is WorkResult.Success)
        assertTrue(WorkResult.failure() is WorkResult.Failure)
        assertTrue(WorkResult.retry() is WorkResult.Retry)
        val data = workDataOf("k" to 1)
        val s = WorkResult.success(data) as WorkResult.Success
        assertEquals(data, s.outputData)
    }

    // --- WorkInfo data class ---------------------------------------------

    @Test
    fun workInfo_defaults_areSane() {
        val id = Uuid.random()
        val info = WorkInfo(id = id, state = WorkInfo.State.ENQUEUED)
        assertEquals(id, info.id)
        assertEquals(WorkProgress.NONE, info.progress)
        assertEquals(WorkData.EMPTY, info.outputData)
        assertTrue(info.tags.isEmpty())
        assertEquals(0, info.runAttemptCount)
        assertFalse(info.isFinished)
    }

    @Test
    fun workInfo_copy_overridesFields() {
        val id = Uuid.random()
        val a = WorkInfo(id = id, state = WorkInfo.State.ENQUEUED)
        val b = a.copy(state = WorkInfo.State.RUNNING, runAttemptCount = 2)
        assertEquals(WorkInfo.State.RUNNING, b.state)
        assertEquals(2, b.runAttemptCount)
        assertNotEquals(a, b)
        assertEquals(a.hashCode(), a.copy().hashCode())
    }

    @Test
    fun workInfo_componentN_returnsAllFields() {
        val id = Uuid.random()
        val info = WorkInfo(
            id = id,
            state = WorkInfo.State.RUNNING,
            progress = WorkProgress(33),
            outputData = workDataOf("k" to "v"),
            tags = setOf("a"),
            runAttemptCount = 4,
        )
        assertEquals(id, info.component1())
        assertEquals(WorkInfo.State.RUNNING, info.component2())
        assertEquals(WorkProgress(33), info.component3())
        assertEquals("v", info.component4().getString("k"))
        assertEquals(setOf("a"), info.component5())
        assertEquals(4, info.component6())
    }

    @Test
    fun workInfo_toString_containsState() {
        val info = WorkInfo(id = Uuid.random(), state = WorkInfo.State.SUCCEEDED)
        assertTrue("SUCCEEDED" in info.toString())
    }

    @Test
    fun workInfoState_entries_hasSixValues() {
        assertEquals(6, WorkInfo.State.entries.size)
    }

    @Test
    fun workInfoState_valueOf_roundTrip() {
        for (entry in WorkInfo.State.entries) {
            assertEquals(entry, WorkInfo.State.valueOf(entry.name))
        }
    }

    @Test
    fun workInfoState_isFinished_isTerminalOnly() {
        assertTrue(WorkInfo.State.SUCCEEDED.isFinished)
        assertTrue(WorkInfo.State.FAILED.isFinished)
        assertTrue(WorkInfo.State.CANCELLED.isFinished)
        assertFalse(WorkInfo.State.ENQUEUED.isFinished)
        assertFalse(WorkInfo.State.RUNNING.isFinished)
        assertFalse(WorkInfo.State.BLOCKED.isFinished)
    }

    // --- OutOfQuotaPolicy ------------------------------------------------

    @Test
    fun outOfQuotaPolicy_entries_hasTwoValues() {
        assertEquals(2, OutOfQuotaPolicy.entries.size)
        assertTrue(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST in OutOfQuotaPolicy.entries)
        assertTrue(OutOfQuotaPolicy.DROP_WORK_REQUEST in OutOfQuotaPolicy.entries)
    }

    @Test
    fun outOfQuotaPolicy_valueOf_roundTrip() {
        for (entry in OutOfQuotaPolicy.entries) {
            assertEquals(entry, OutOfQuotaPolicy.valueOf(entry.name))
        }
    }

    // --- BackgroundCapabilities ------------------------------------------

    @Test
    fun backgroundCapabilities_dataClassOps() {
        val caps = BackgroundCapabilities(supportsPersistence = true, supportsOsScheduling = false)
        assertTrue(caps.component1())
        assertFalse(caps.component2())
        val copied = caps.copy(supportsOsScheduling = true)
        assertTrue(copied.supportsOsScheduling)
        assertNotEquals(caps, copied)
        assertEquals(
            caps,
            BackgroundCapabilities(supportsPersistence = true, supportsOsScheduling = false),
        )
        assertTrue("BackgroundCapabilities" in caps.toString())
    }

    // --- Exceptions ------------------------------------------------------

    @Test
    fun workEnqueueException_carriesMessageAndCause() {
        val cause = IllegalStateException("under")
        val ex = WorkEnqueueException("scheduler rejected", cause)
        assertEquals("scheduler rejected", ex.message)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun workEnqueueException_defaultCauseIsNull() {
        val ex = WorkEnqueueException("rejected")
        assertEquals("rejected", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun workerInstantiationException_carriesMessageAndCause() {
        val cause = NoSuchElementException("missing")
        val ex = WorkerInstantiationException("no factory", cause)
        assertEquals("no factory", ex.message)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun workerInstantiationException_defaultCauseIsNull() {
        val ex = WorkerInstantiationException("nope")
        assertNull(ex.cause)
    }

    @Test
    fun workDataSerializationException_carriesMessageAndCause() {
        val cause = IllegalArgumentException("bad")
        val ex = WorkDataSerializationException("cannot serialize", cause)
        assertEquals("cannot serialize", ex.message)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun workDataSerializationException_defaultCauseIsNull() {
        val ex = WorkDataSerializationException("oops")
        assertNull(ex.cause)
    }

    // --- WorkEvent variants ----------------------------------------------

    @Test
    fun workEvent_enqueued_dataClassOps() {
        val id = Uuid.random()
        val data = workDataOf("k" to 1)
        val e = WorkEvent.Enqueued(id = id, tag = "sync", inputData = data)
        assertEquals(id, e.component1())
        assertEquals("sync", e.component2())
        assertEquals(data, e.component3())
        val copied = e.copy(tag = null)
        assertNull(copied.tag)
        assertNotEquals(e, copied)
        assertEquals(e, WorkEvent.Enqueued(id = id, tag = "sync", inputData = data))
        assertTrue("Enqueued" in e.toString())
    }

    @Test
    fun workEvent_started_dataClassOps() {
        val id = Uuid.random()
        val e = WorkEvent.Started(id = id, attemptCount = 3)
        assertEquals(id, e.component1())
        assertEquals(3, e.component2())
        assertEquals(3, e.attemptCount)
        val copied = e.copy(attemptCount = 5)
        assertEquals(5, copied.attemptCount)
        assertNotEquals(e, copied)
        assertEquals(e, WorkEvent.Started(id = id, attemptCount = 3))
        assertTrue("Started" in e.toString())
    }

    @Test
    fun workEvent_progress_dataClassOps() {
        val id = Uuid.random()
        val p = WorkProgress(40)
        val e = WorkEvent.Progress(id = id, progress = p)
        assertEquals(id, e.component1())
        assertEquals(p, e.component2())
        val copied = e.copy(progress = WorkProgress(80))
        assertNotEquals(e, copied)
        assertEquals(80, copied.progress.progress)
        assertTrue("Progress" in e.toString())
        assertEquals(e.hashCode(), WorkEvent.Progress(id = id, progress = p).hashCode())
    }

    @Test
    fun workEvent_resulted_dataClassOps() {
        val id = Uuid.random()
        val res = WorkResult.success()
        val e = WorkEvent.Resulted(id = id, result = res, durationMs = 42L)
        assertEquals(id, e.component1())
        assertEquals(res, e.component2())
        assertEquals(42L, e.component3())
        val copied = e.copy(durationMs = 100L)
        assertEquals(100L, copied.durationMs)
        assertNotEquals(e, copied)
        assertTrue("Resulted" in e.toString())
    }

    @Test
    fun workEvent_sealedHierarchy_idAccessor() {
        val id = Uuid.random()
        val events = listOf<WorkEvent>(
            WorkEvent.Enqueued(id, null, WorkData.EMPTY),
            WorkEvent.Started(id, 1),
            WorkEvent.Progress(id, WorkProgress.NONE),
            WorkEvent.Resulted(id, WorkResult.success(), 0L),
        )
        for (e in events) {
            assertEquals(id, e.id)
        }
    }

    // --- WorkerConfig + sub-configs --------------------------------------

    @Test
    fun workerConfig_defaults_useFactoryDefaults() {
        val cfg = WorkerConfig()
        assertEquals(LogLevel.WARN, cfg.logLevel)
        assertEquals(RetryConfig.DEFAULT, cfg.defaultRetryConfig)
        assertTrue(cfg.observers.isEmpty())
        assertEquals(AndroidWorkerConfig(), cfg.androidConfig)
        assertEquals(IosWorkerConfig(), cfg.iosConfig)
        assertEquals(DesktopWorkerConfig(), cfg.desktopConfig)
        assertEquals(WebWorkerConfig(), cfg.webConfig)
    }

    @Test
    fun workerConfig_customValues_persist() {
        val obs = LoggingWorkObserver(tag = "test")
        val cfg = WorkerConfig(
            logLevel = LogLevel.DEBUG,
            defaultRetryConfig = RetryConfig.AGGRESSIVE,
            observers = listOf(obs),
            androidConfig = AndroidWorkerConfig(useReflectionFactory = false),
            iosConfig = IosWorkerConfig(enableBackgroundTasks = true),
            desktopConfig = DesktopWorkerConfig(maxConcurrentWorkers = 8),
            webConfig = WebWorkerConfig(enableBackgroundSync = true),
        )
        assertEquals(LogLevel.DEBUG, cfg.logLevel)
        assertEquals(RetryConfig.AGGRESSIVE, cfg.defaultRetryConfig)
        assertEquals(1, cfg.observers.size)
        assertFalse(cfg.androidConfig.useReflectionFactory)
        assertTrue(cfg.iosConfig.enableBackgroundTasks)
        assertEquals(8, cfg.desktopConfig.maxConcurrentWorkers)
        assertTrue(cfg.webConfig.enableBackgroundSync)
    }

    @Test
    fun workerConfig_copyAndComponents() {
        val cfg = WorkerConfig()
        val copied = cfg.copy(logLevel = LogLevel.VERBOSE)
        assertEquals(LogLevel.VERBOSE, copied.logLevel)
        assertNotEquals(cfg, copied)
        assertEquals(LogLevel.WARN, cfg.component1())
        assertEquals(RetryConfig.DEFAULT, cfg.component2())
        assertTrue(cfg.component3().isEmpty())
        assertEquals(AndroidWorkerConfig(), cfg.component4())
        assertEquals(IosWorkerConfig(), cfg.component5())
        assertEquals(DesktopWorkerConfig(), cfg.component6())
        assertEquals(WebWorkerConfig(), cfg.component7())
    }

    @Test
    fun logLevel_entries_hasSixValues() {
        assertEquals(6, LogLevel.entries.size)
    }

    @Test
    fun logLevel_valueOf_roundTrip() {
        for (entry in LogLevel.entries) {
            assertEquals(entry, LogLevel.valueOf(entry.name))
        }
    }

    @Test
    fun androidWorkerConfig_defaultsAndCustom() {
        val default = AndroidWorkerConfig()
        assertNull(default.notificationChannelId)
        assertNull(default.notificationChannelName)
        assertTrue(default.useReflectionFactory)
        val custom = AndroidWorkerConfig(
            notificationChannelId = "ch",
            notificationChannelName = "Worker",
            useReflectionFactory = false,
        )
        assertEquals("ch", custom.notificationChannelId)
        assertEquals("Worker", custom.notificationChannelName)
        assertFalse(custom.useReflectionFactory)
        val copied = custom.copy(notificationChannelId = "ch2")
        assertEquals("ch2", copied.notificationChannelId)
        assertNotEquals(custom, copied)
        assertEquals("ch", custom.component1())
        assertEquals("Worker", custom.component2())
        assertFalse(custom.component3())
        assertTrue("AndroidWorkerConfig" in custom.toString())
    }

    @Test
    fun iosWorkerConfig_defaultsAndCustom() {
        val default = IosWorkerConfig()
        assertFalse(default.enableBackgroundTasks)
        assertEquals("", default.bgProcessingTaskIdentifier)
        assertTrue(default.enablePersistence)
        assertEquals("worker-kmp-ios", default.persistenceKey)
        assertEquals("", default.appRefreshTaskIdentifier)
        val custom = IosWorkerConfig(
            enableBackgroundTasks = true,
            bgProcessingTaskIdentifier = "com.example.bg",
            enablePersistence = false,
            persistenceKey = "k",
            appRefreshTaskIdentifier = "com.example.refresh",
        )
        assertTrue(custom.enableBackgroundTasks)
        assertEquals("com.example.bg", custom.bgProcessingTaskIdentifier)
        assertFalse(custom.enablePersistence)
        assertEquals("k", custom.persistenceKey)
        assertEquals("com.example.refresh", custom.appRefreshTaskIdentifier)
        val copied = custom.copy(persistenceKey = "k2")
        assertNotEquals(custom, copied)
        assertEquals(custom.component1(), true)
        assertEquals(custom.component2(), "com.example.bg")
        assertTrue("IosWorkerConfig" in custom.toString())
    }

    @Test
    fun desktopWorkerConfig_defaultsAndCustom() {
        val default = DesktopWorkerConfig()
        assertEquals(4, default.maxConcurrentWorkers)
        assertTrue(default.persistenceEnabled)
        assertNull(default.persistencePath)
        assertEquals(5_000L, default.constraintCheckIntervalMs)
        assertNull(default.background)
        val bg = DesktopBackgroundConfig(appId = "app", daemonJarPath = "/x.jar")
        val custom = DesktopWorkerConfig(
            maxConcurrentWorkers = 8,
            persistenceEnabled = false,
            persistencePath = "/tmp/w",
            constraintCheckIntervalMs = 1000L,
            background = bg,
        )
        assertEquals(8, custom.maxConcurrentWorkers)
        assertFalse(custom.persistenceEnabled)
        assertEquals("/tmp/w", custom.persistencePath)
        assertEquals(1000L, custom.constraintCheckIntervalMs)
        assertEquals(bg, custom.background)
        val copied = custom.copy(maxConcurrentWorkers = 2)
        assertNotEquals(custom, copied)
        assertEquals(custom.component1(), 8)
        assertTrue("DesktopWorkerConfig" in custom.toString())
    }

    @Test
    fun desktopBackgroundConfig_defaultsAndCustom() {
        val cfg = DesktopBackgroundConfig(appId = "com.example", daemonJarPath = "/opt/d.jar")
        assertEquals("com.example", cfg.appId)
        assertEquals("/opt/d.jar", cfg.daemonJarPath)
        assertNull(cfg.runtimeJavaHome)
        assertNull(cfg.persistenceDir)
        assertEquals(15, cfg.pollIntervalMin)
        assertTrue(cfg.installOnFirstRun)
        assertTrue(cfg.uninstallOnAppUninstall)
        assertFalse(cfg.runOnlyIfLoggedOn)
        val custom = DesktopBackgroundConfig(
            appId = "id",
            daemonJarPath = "/d",
            runtimeJavaHome = "/jh",
            persistenceDir = "/pd",
            pollIntervalMin = 5,
            installOnFirstRun = false,
            uninstallOnAppUninstall = false,
            runOnlyIfLoggedOn = true,
        )
        assertEquals("/jh", custom.runtimeJavaHome)
        assertEquals("/pd", custom.persistenceDir)
        assertEquals(5, custom.pollIntervalMin)
        assertFalse(custom.installOnFirstRun)
        assertFalse(custom.uninstallOnAppUninstall)
        assertTrue(custom.runOnlyIfLoggedOn)
        val copied = custom.copy(pollIntervalMin = 30)
        assertNotEquals(custom, copied)
        assertEquals("id", custom.component1())
        assertEquals("/d", custom.component2())
        assertEquals("/jh", custom.component3())
        assertEquals("/pd", custom.component4())
        assertEquals(5, custom.component5())
        assertFalse(custom.component6())
        assertFalse(custom.component7())
        assertTrue(custom.component8())
        assertTrue("DesktopBackgroundConfig" in custom.toString())
    }

    @Test
    fun webWorkerConfig_defaultsAndCustom() {
        val default = WebWorkerConfig()
        assertTrue(default.enablePersistence)
        assertEquals("worker-kmp", default.persistenceDbName)
        assertEquals(5_000L, default.constraintCheckIntervalMs)
        assertFalse(default.enableBackgroundSync)
        assertEquals("/worker-kmp-sw.js", default.serviceWorkerScript)
        assertFalse(default.enablePeriodicBackgroundSync)
        val custom = WebWorkerConfig(
            enablePersistence = false,
            persistenceDbName = "db",
            constraintCheckIntervalMs = 1000L,
            enableBackgroundSync = true,
            serviceWorkerScript = "/sw.js",
            enablePeriodicBackgroundSync = true,
        )
        assertFalse(custom.enablePersistence)
        assertEquals("db", custom.persistenceDbName)
        assertEquals(1000L, custom.constraintCheckIntervalMs)
        assertTrue(custom.enableBackgroundSync)
        assertEquals("/sw.js", custom.serviceWorkerScript)
        assertTrue(custom.enablePeriodicBackgroundSync)
        val copied = custom.copy(persistenceDbName = "db2")
        assertNotEquals(custom, copied)
        assertFalse(custom.component1())
        assertEquals("db", custom.component2())
        assertTrue("WebWorkerConfig" in custom.toString())
    }

    // --- LoggingWorkObserver — exercises every WorkEvent branch ---------

    @Test
    fun loggingWorkObserver_handlesAllEvents() = runTest {
        val obs = LoggingWorkObserver()
        val id = Uuid.random()
        // Each call exercises one branch of the when-expression in onEvent.
        obs.onEvent(WorkEvent.Enqueued(id = id, tag = "sync", inputData = WorkData.EMPTY))
        obs.onEvent(WorkEvent.Enqueued(id = id, tag = null, inputData = WorkData.EMPTY))
        obs.onEvent(WorkEvent.Started(id = id, attemptCount = 1))
        obs.onEvent(WorkEvent.Progress(id = id, progress = WorkProgress(50)))
        obs.onEvent(WorkEvent.Resulted(id = id, result = WorkResult.success(), durationMs = 10L))
        obs.onEvent(WorkEvent.Resulted(id = id, result = WorkResult.failure("oops"), durationMs = 11L))
        obs.onEvent(WorkEvent.Resulted(id = id, result = WorkResult.failure(""), durationMs = 12L))
        obs.onEvent(WorkEvent.Resulted(id = id, result = WorkResult.retry("transient"), durationMs = 13L))
    }

    @Test
    fun loggingWorkObserver_customTag() = runTest {
        val obs = LoggingWorkObserver(tag = "custom-tag")
        assertNotNull(obs)
        // Smoke-test that custom-tagged observer also dispatches without throwing.
        obs.onEvent(WorkEvent.Started(id = Uuid.random(), attemptCount = 2))
    }

    // --- CoroutineWorker accessors + setProgress -------------------------

    @Test
    fun coroutineWorker_accessorsDelegateToContext() = runTest {
        val id = Uuid.random()
        val data = workDataOf("k" to "v")
        val tags = setOf("t1", "t2")
        val ctx = TestWorkerContext(id = id, inputData = data, tags = tags)
        val worker = object : CoroutineWorker(ctx) {
            override suspend fun doWork(): WorkResult = WorkResult.success()
            suspend fun reportProgress(p: WorkProgress) = setProgress(p)
            fun ctxId() = this.id
            fun ctxInputData() = this.inputData
            fun ctxTags() = this.tags
        }
        assertEquals(id, worker.ctxId())
        assertEquals(data, worker.ctxInputData())
        assertEquals(tags, worker.ctxTags())

        worker.reportProgress(WorkProgress(20))
        worker.reportProgress(WorkProgress(80))
        assertEquals(listOf(WorkProgress(20), WorkProgress(80)), ctx.progressUpdates)

        // Exercise doWork() once for line coverage on the worker body.
        assertTrue(worker.doWork() is WorkResult.Success)
    }
}
