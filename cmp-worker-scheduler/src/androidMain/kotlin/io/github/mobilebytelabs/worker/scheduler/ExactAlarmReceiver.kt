package io.github.mobilebytelabs.worker.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Broadcast receiver fired by AlarmManager at the requested exact instant. Reads the
 * worker class name from the Intent extras (set by [ExactAlarmScheduler] at schedule
 * time) and enqueues a OneTimeWorkRequest with that class name via the Koin-bound
 * [WorkManager].
 *
 * Consumer AndroidManifest must declare:
 *
 * ```xml
 * <receiver
 *     android:name="io.github.mobilebytelabs.worker.scheduler.ExactAlarmReceiver"
 *     android:exported="false" />
 * ```
 */
class ExactAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val workerClassName = intent.getStringExtra(EXTRA_WORKER_CLASS_NAME) ?: return
        val workManager: WorkManager = KoinPlatform.getKoin().get()
        // T : AbstractDataSyncWorker satisfies the OneTimeWorkRequestBuilder bound at the
        // type level; the runtime worker-class lookup uses the supplied String.
        val request = OneTimeWorkRequestBuilder<AbstractDataSyncWorker>(workerClassName).apply {
            setInputData(workDataOf())
            addTag(SYNC_WORK_NAME)
        }.build()
        scope.launch { workManager.enqueue(request) }
    }

    companion object {
        const val EXTRA_WORKER_CLASS_NAME = "io.github.mobilebytelabs.worker.scheduler.worker_class_name"
    }
}
