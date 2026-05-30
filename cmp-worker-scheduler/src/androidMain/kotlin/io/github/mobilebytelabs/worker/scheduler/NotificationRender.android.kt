package io.github.mobilebytelabs.worker.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.koin.mp.KoinPlatform

private const val DEFAULT_CHANNEL_ID = "default"
private const val DEFAULT_CHANNEL_NAME = "Notifications"

/**
 * Android actual: NotificationCompat.Builder + NotificationManagerCompat.notify.
 *
 * Channel creation is idempotent (Android 8+ requirement; older versions no-op).
 * Notification ID derived from content.title.hashCode (stable per title, so re-rendering
 * the "same" notification replaces the old one rather than stacking duplicates).
 *
 * POST_NOTIFICATIONS runtime permission (Android 13+ / API 33+) is the caller's
 * responsibility — request via ActivityCompat.requestPermissions before scheduling.
 */
actual fun renderNotification(content: NotificationContent) {
    val context: Context = KoinPlatform.getKoin().get()
    val channelId = content.channelId ?: DEFAULT_CHANNEL_ID
    ensureChannel(context, channelId)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(content.title)
        .setContentText(content.body)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(content.title.hashCode(), notification)
}

private fun ensureChannel(context: Context, channelId: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(channelId) != null) return
    val channel = NotificationChannel(channelId, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)
}
