package io.github.mobilebytelabs.worker.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mobilebytelabs.worker.workDataOf
import org.koin.mp.KoinPlatform

/**
 * Broadcast receiver fired by AlarmManager at the requested exact instant.
 * Delegates to the Koin-bound WorkScheduler to enqueue an immediate data sync.
 *
 * Consumer AndroidManifest must declare:
 *   <receiver
 *       android:name="io.github.mobilebytelabs.worker.scheduler.ExactAlarmReceiver"
 *       android:exported="false" />
 */
class ExactAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler: WorkScheduler = KoinPlatform.getKoin().get()
        scheduler.enqueueDataSync(mode = WorkMode.Background, payload = workDataOf())
    }
}
