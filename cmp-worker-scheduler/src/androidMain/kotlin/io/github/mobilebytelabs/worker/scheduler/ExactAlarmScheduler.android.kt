package io.github.mobilebytelabs.worker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Android actual: `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)`.
 *
 * Requires SCHEDULE_EXACT_ALARM permission in the consumer-app AndroidManifest.xml
 * (Android 12+ / API 31+; user-grantable). When permission is denied via
 * `AlarmManager.canScheduleExactAlarms()`, falls back to the supplied [fallback]
 * WorkScheduler's flex-window `scheduleDataSyncAt` — battery-friendly degradation.
 *
 * [Context] is resolved via [KoinPlatform] so consumers don't have to thread it through
 * the API surface — same pattern as cmp-worker-android's other Context-dependent types.
 */
@OptIn(ExperimentalTime::class)
actual class ExactAlarmScheduler actual constructor(private val fallback: WorkScheduler) {
    actual fun <W : AbstractDataSyncWorker> scheduleExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle {
        val context: Context = KoinPlatform.getKoin().get()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(
                "ExactAlarmScheduler",
                "SCHEDULE_EXACT_ALARM permission not granted; falling back to flex-window scheduleDataSyncAt",
            )
            fallback.scheduleDataSyncAt(workerClass, instant, mode, payload)
        } else {
            val workerClassName = workerClass.simpleName
                ?: error("ExactAlarmScheduler requires a named subclass of AbstractDataSyncWorker")
            val pendingIntent = buildPendingIntent(context, instant, workerClassName)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                instant.toEpochMilliseconds(),
                pendingIntent,
            )
            WorkHandle(uniqueName = "exact-sync-${instant.toEpochMilliseconds()}")
        }
    }

    private fun buildPendingIntent(context: Context, instant: Instant, workerClassName: String): PendingIntent {
        val intent = Intent(context, ExactAlarmReceiver::class.java).apply {
            putExtra("trigger_ms", instant.toEpochMilliseconds())
            putExtra(ExactAlarmReceiver.EXTRA_WORKER_CLASS_NAME, workerClassName)
        }
        return PendingIntent.getBroadcast(
            context,
            instant.toEpochMilliseconds().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
