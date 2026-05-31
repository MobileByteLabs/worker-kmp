package io.github.mobilebytelabs.worker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.mobilebytelabs.worker.WorkData
import kotlinx.datetime.Instant
import org.koin.mp.KoinPlatform

/**
 * Android actual: AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...).
 *
 * Requires SCHEDULE_EXACT_ALARM permission in the consumer-app AndroidManifest.xml
 * (Android 12+ / API 31+; user-grantable). When permission is denied via
 * AlarmManager.canScheduleExactAlarms(), falls back to the supplied [fallback]
 * WorkScheduler's flex-window scheduleDataSyncAt — battery-friendly degradation.
 *
 * Context is resolved via KoinPlatform so consumers don't have to thread it through
 * the API surface — same pattern as cmp-worker-android's other Context-dependent types.
 *
 * Caveats:
 * - Doze: setExactAndAllowWhileIdle WILL fire during Doze (the "AndAllowWhileIdle" suffix
 *   wakes the device). User-throttled per ~9-minute interval; consecutive exact alarms
 *   within that window may be delayed.
 * - Battery: prefer this for user-visible scheduled actions (e.g., reminders); for
 *   sync-only work, flex-window via scheduleDataSyncAt is more battery-respectful.
 */
actual class ExactAlarmScheduler actual constructor(private val fallback: WorkScheduler) {
    actual fun scheduleExact(instant: Instant, mode: WorkMode, payload: WorkData): WorkHandle {
        val context: Context = KoinPlatform.getKoin().get()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(
                "ExactAlarmScheduler",
                "SCHEDULE_EXACT_ALARM permission not granted; falling back to flex-window scheduleDataSyncAt",
            )
            fallback.scheduleDataSyncAt(instant, mode, payload)
        } else {
            val pendingIntent = buildPendingIntent(context, instant)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                instant.toEpochMilliseconds(),
                pendingIntent,
            )
            WorkHandle(uniqueName = "exact-sync-${instant.toEpochMilliseconds()}")
        }
    }

    private fun buildPendingIntent(context: Context, instant: Instant): PendingIntent {
        val intent = Intent(context, ExactAlarmReceiver::class.java).apply {
            putExtra("trigger_ms", instant.toEpochMilliseconds())
        }
        return PendingIntent.getBroadcast(
            context,
            instant.toEpochMilliseconds().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
