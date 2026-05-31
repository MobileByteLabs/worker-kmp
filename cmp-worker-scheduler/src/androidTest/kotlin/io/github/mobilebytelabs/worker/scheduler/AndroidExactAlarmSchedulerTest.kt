package io.github.mobilebytelabs.worker.scheduler

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Instrumented test: verifies [ExactAlarmScheduler] wiring against a real [AlarmManager].
 * Robolectric/Android-test runtime — AlarmManager is the shadow version in test, but
 * the API surface (`setExactAndAllowWhileIdle`, `canScheduleExactAlarms`) is unchanged.
 */
@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class AndroidExactAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var fallback: WorkScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (GlobalContext.getOrNull() != null) stopKoin()
        fallback = RecordingFallback()
        startKoin {
            modules(
                module {
                    single { context }
                    single<WorkScheduler> { fallback }
                },
            )
        }
    }

    @Test
    fun scheduleExact_returnsHandleWithExactSyncPrefix() {
        val scheduler = ExactAlarmScheduler(fallback)
        val instant = Clock.System.now() + 5.minutes
        val handle = scheduler.scheduleExact<FakeSyncWorker>(instant, WorkMode.Background, workDataOf("k" to "v"))
        assertNotNull(handle)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            assertEquals("exact-sync-${instant.toEpochMilliseconds()}", handle.uniqueName)
        }
    }

    @Test
    fun scheduleExact_whenPermissionDenied_delegatesToFallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return
        val scheduler = ExactAlarmScheduler(fallback)
        val instant = Clock.System.now() + 5.minutes
        scheduler.scheduleExact<FakeSyncWorker>(instant, WorkMode.Background, workDataOf())
        val recording = fallback as RecordingFallback
        assertEquals(1, recording.scheduleDataSyncAtCalls)
    }
}

/** Concrete subclass used only to satisfy `KClass<W : AbstractDataSyncWorker>` at the call site. */
private class FakeSyncWorker(ctx: WorkerContext) :
    AbstractDataSyncWorker(ctx, syncables = emptyList(), persister = SyncStatePersister())

@OptIn(ExperimentalTime::class)
private class RecordingFallback : WorkScheduler {
    var scheduleDataSyncAtCalls = 0

    override fun <W : AbstractDataSyncWorker> enqueueDataSync(
        workerClass: KClass<W>,
        mode: WorkMode,
        payload: WorkData,
    ) = WorkHandle(uniqueName = "fallback-enqueue")

    override fun <W : AbstractDataSyncWorker> scheduleDailyDataSync(
        workerClass: KClass<W>,
        timeOfDay: LocalTime,
        timeZone: TimeZone,
        payload: WorkData,
    ) = WorkHandle(uniqueName = "fallback-daily")

    override fun <W : AbstractDataSyncWorker> schedulePeriodicDataSync(
        workerClass: KClass<W>,
        interval: kotlin.time.Duration,
        initialDelay: kotlin.time.Duration,
        payload: WorkData,
    ) = WorkHandle(uniqueName = "fallback-periodic")

    override fun <W : AbstractDataSyncWorker> scheduleDataSyncAt(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle {
        scheduleDataSyncAtCalls++
        return WorkHandle(uniqueName = "fallback-at")
    }

    override fun <W : AbstractDataSyncWorker> scheduleDataSyncAtExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ) = WorkHandle(uniqueName = "fallback-at-exact")

    override fun observeWork(name: String): Flow<WorkStatus> = flowOf(WorkStatus.Pending)
    override fun cancelWork(name: String) {}
}
