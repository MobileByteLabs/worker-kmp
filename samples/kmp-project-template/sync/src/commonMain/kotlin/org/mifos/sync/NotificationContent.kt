package org.mifos.sync

/**
 * Consumer-defined notification payload — sample-side type. The cmp-worker-scheduler
 * library is scheduling-only; rendering is the consumer's responsibility.
 *
 * Carried via the WorkRequest's inputData (title / body / channelId keys) and
 * unpacked inside [NotificationWorker.doWork] before the platform actual renders it.
 */
data class NotificationContent(
    val title: String,
    val body: String,
    val channelId: String? = null,
)
