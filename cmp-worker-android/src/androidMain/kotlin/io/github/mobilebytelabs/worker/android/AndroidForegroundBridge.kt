@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo as AndroidForegroundInfo
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.ExperimentalForegroundApi
import io.github.mobilebytelabs.worker.ForegroundInfo
import io.github.mobilebytelabs.worker.ForegroundServiceType
import io.github.mobilebytelabs.worker.ForegroundWorker
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

/**
 * Android-side bridge invoked reflectively by `cmp-worker-kmp`'s JVM actual of
 * `runAsForeground` when the JVM runtime is detected as Android.
 *
 * `cmp-worker-kmp` ships a single JVM artifact (no `android()` target — see Phase 1
 * alpha01.X deviation note in `CHANGELOG.md`), so Android consumers receive the
 * JVM-targeted `runAsForeground` actual at runtime. That JVM actual detects Android
 * via `System.getProperty("java.runtime.name")` and reflectively calls into
 * [promote] here — keeping Android dependencies out of `cmp-worker-kmp`.
 *
 * Lifecycle: [KmpAndroidWorker] registers itself in [activeWorkers] before delegating
 * to the KMP worker's `doWork()`, and unregisters in a `try/finally`. When the KMP
 * worker calls `setForeground(info)`, this bridge looks up the registered
 * androidx.work `CoroutineWorker` by `worker.id` and calls `setForegroundAsync` on it.
 *
 * Added in v3.0.0-alpha01.X (Phase 1 deep impl).
 */
@ExperimentalForegroundApi
public object AndroidForegroundBridge {

    internal const val DEFAULT_CHANNEL_ID: String = "worker-kmp.foreground"
    private const val DEFAULT_CHANNEL_NAME: String = "worker-kmp Foreground Tasks"
    private const val DEFAULT_CHANNEL_DESCRIPTION: String = "Long-running background work from worker-kmp"
    private const val PROGRESS_MAX: Int = 100

    // Reflectively invoked from cmp-worker-kmp's JVM actual.
    private val activeWorkers = ConcurrentHashMap<Uuid, KmpAndroidWorker>()

    /**
     * Called by [KmpAndroidWorker] before it delegates to the user's KMP worker.
     * Pairs with [unregister] in a try/finally.
     */
    internal fun register(id: Uuid, worker: KmpAndroidWorker) {
        activeWorkers[id] = worker
    }

    /**
     * Called by [KmpAndroidWorker] after the user's KMP worker returns (or throws).
     */
    internal fun unregister(id: Uuid) {
        activeWorkers.remove(id)
    }

    /**
     * Reflectively invoked from `cmp-worker-kmp`'s JVM actual `runAsForeground` when
     * an Android runtime is detected. Looks up the live [KmpAndroidWorker] for
     * [worker.id] and promotes it to a foreground service.
     *
     * Silently no-ops with a kermit WARN if no live worker is registered for the id
     * (worker invoked `setForeground` outside its `doWork()` lifetime, or the
     * androidx.work bridge isn't running this worker — e.g. an in-process test).
     */
    @JvmStatic
    public fun promote(worker: ForegroundWorker, info: ForegroundInfo) {
        val androidWorker = activeWorkers[worker.id]
        if (androidWorker == null) {
            Logger.withTag("worker-kmp.foreground.android").w {
                "AndroidForegroundBridge.promote(id=${worker.id}) — no live KmpAndroidWorker registered. " +
                    "setForeground() called outside doWork() lifetime?"
            }
            return
        }
        val context = androidWorker.applicationContext
        runCatching {
            ensureNotificationChannel(context)
            val notification = buildNotification(context, info)
            val androidInfo = buildAndroidForegroundInfo(info.notificationId, notification, info.serviceType)
            androidWorker.setForegroundAsync(androidInfo)
            Logger.withTag("worker-kmp.foreground.android").d {
                "Android foreground promotion dispatched: id=${worker.id} title=${info.title} " +
                    "progress=${info.progress.progress} serviceType=${info.serviceType}"
            }
        }.onFailure { t ->
            Logger.withTag("worker-kmp.foreground.android").w(t) {
                "Android foreground promotion failed for id=${worker.id}"
            }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = DEFAULT_CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(context: Context, info: ForegroundInfo): Notification {
        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setContentTitle(info.title)
            .setContentText(info.message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        val pct = info.progress.progress
        if (pct in 1..PROGRESS_MAX) {
            builder.setProgress(PROGRESS_MAX, pct, false)
        } else if (pct == 0) {
            builder.setProgress(PROGRESS_MAX, 0, true)
        }
        return builder.build()
    }

    private fun buildAndroidForegroundInfo(
        notificationId: Int,
        notification: Notification,
        serviceType: ForegroundServiceType?,
    ): AndroidForegroundInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || serviceType == null) {
            return AndroidForegroundInfo(notificationId, notification)
        }
        val typeConstant = serviceType.toAndroidConstantOrNull()
        return if (typeConstant != null) {
            AndroidForegroundInfo(notificationId, notification, typeConstant)
        } else {
            AndroidForegroundInfo(notificationId, notification)
        }
    }
}

/**
 * Maps a [ForegroundServiceType] to the matching `android.content.pm.ServiceInfo`
 * constant for the current SDK level. Returns `null` when the type is not available
 * on the current API level — caller falls back to the no-serviceType
 * `androidx.work.ForegroundInfo` constructor (Android <14 doesn't enforce service types).
 *
 * Added in v3.0.0-alpha01.X.
 */
@ExperimentalForegroundApi
internal fun ForegroundServiceType.toAndroidConstantOrNull(): Int? {
    val api29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val api30 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val api34 = Build.VERSION.SDK_INT >= 34
    if (!api29) return null
    return when (this) {
        ForegroundServiceType.DATA_SYNC -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else null
        ForegroundServiceType.MEDIA_PLAYBACK -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else null
        ForegroundServiceType.MEDIA_PROJECTION -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else null
        ForegroundServiceType.CONNECTED_DEVICE -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else null
        ForegroundServiceType.PHONE_CALL -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else null
        ForegroundServiceType.CAMERA -> if (api30) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else null
        ForegroundServiceType.MICROPHONE -> if (api30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else null
        ForegroundServiceType.LOCATION -> if (api29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else null
        ForegroundServiceType.HEALTH -> if (api34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
        ForegroundServiceType.REMOTE_MESSAGING -> if (api34) ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING else null
        ForegroundServiceType.SHORT_SERVICE -> if (api34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else null
        ForegroundServiceType.SPECIAL_USE -> if (api34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else null
        ForegroundServiceType.SYSTEM_EXEMPTED -> if (api34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else null
    }
}
