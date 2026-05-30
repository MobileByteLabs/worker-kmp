package io.github.mobilebytelabs.worker.scheduler

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes

/**
 * Instrumented test: verifies ExactAlarmScheduler wiring against a real AlarmManager.
 * Robolectric/Android-test runtime — AlarmManager is the shadow version in test, but
 * the API surface (setExactAndAllowWhileIdle, canScheduleExactAlarms) is unchanged.
 */
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
        val handle = scheduler.scheduleExact(instant, WorkMode.Background, workDataOf("k" to "v"))
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
        scheduler.scheduleExact(instant, WorkMode.Background, workDataOf())
        val recording = fallback as RecordingFallback
        assertEquals(1, recording.scheduleDataSyncAtCalls)
    }
}

private class RecordingFallback : WorkScheduler {
    var scheduleDataSyncAtCalls = 0
    override fun enqueueDataSync(mode: WorkMode, payload: io.github.mobilebytelabs.worker.WorkData) =
        WorkHandle(uniqueName = "fallback-enqueue")
    override fun scheduleNotification(content: NotificationContent, delay: kotlin.time.Duration, mode: WorkMode) =
        WorkHandle(uniqueName = "fallback-notif")
    override fun scheduleDailyDataSync(
        timeOfDay: kotlinx.datetime.LocalTime,
        timeZone: kotlinx.datetime.TimeZone,
        payload: io.github.mobilebytelabs.worker.WorkData,
    ) = WorkHandle(uniqueName = "fallback-daily")
    override fun schedulePeriodicDataSync(
        interval: kotlin.time.Duration,
        initialDelay: kotlin.time.Duration,
        payload: io.github.mobilebytelabs.worker.WorkData,
    ) = WorkHandle(uniqueName = "fallback-periodic")
    override fun scheduleDataSyncAt(
        instant: kotlinx.datetime.Instant,
        mode: WorkMode,
        payload: io.github.mobilebytelabs.worker.WorkData,
    ): WorkHandle {
        scheduleDataSyncAtCalls++
        return WorkHandle(uniqueName = "fallback-at")
    }
    override fun scheduleDataSyncAtExact(
        instant: kotlinx.datetime.Instant,
        mode: WorkMode,
        payload: io.github.mobilebytelabs.worker.WorkData,
    ) = WorkHandle(uniqueName = "fallback-at-exact")
    override fun scheduleNotificationAt(
        content: NotificationContent,
        instant: kotlinx.datetime.Instant,
        mode: WorkMode,
    ) = WorkHandle(uniqueName = "fallback-notif-at")
    override fun observeWork(handle: WorkHandle): kotlinx.coroutines.flow.Flow<WorkStatus> =
        kotlinx.coroutines.flow.flowOf(WorkStatus.Pending)
    override fun cancelWork(handle: WorkHandle) {}
}
