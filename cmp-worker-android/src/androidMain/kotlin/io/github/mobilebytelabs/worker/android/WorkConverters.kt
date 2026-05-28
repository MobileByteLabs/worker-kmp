package io.github.mobilebytelabs.worker.android

import androidx.work.Data
import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.OutOfQuotaPolicy
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlin.uuid.Uuid
import androidx.work.BackoffPolicy as AndroidBackoffPolicy
import androidx.work.Constraints as AndroidConstraints
import androidx.work.ExistingPeriodicWorkPolicy as AndroidExistingPeriodicWorkPolicy
import androidx.work.NetworkType as AndroidNetworkType
import androidx.work.OutOfQuotaPolicy as AndroidOutOfQuotaPolicy
import androidx.work.WorkInfo as AndroidWorkInfo

internal const val KEY_KMP_ID = "kmp_worker_id"
internal const val KEY_KMP_CLASS = "kmp_worker_class"
internal const val TAG_KMP_ID_PREFIX = "kmp_id:"

internal fun Uuid.toTag(): String = "$TAG_KMP_ID_PREFIX$this"

internal fun Set<String>.extractKmpId(): Uuid? = firstOrNull { it.startsWith(TAG_KMP_ID_PREFIX) }
    ?.removePrefix(TAG_KMP_ID_PREFIX)
    ?.let { runCatching { Uuid.parse(it) }.getOrNull() }

internal fun Set<String>.userTags(): Set<String> = filterNot { it.startsWith(TAG_KMP_ID_PREFIX) }.toSet()

internal fun BackoffPolicy.toAndroid(): AndroidBackoffPolicy = when (this) {
    BackoffPolicy.EXPONENTIAL -> AndroidBackoffPolicy.EXPONENTIAL
    BackoffPolicy.LINEAR -> AndroidBackoffPolicy.LINEAR
}

internal fun NetworkType.toAndroid(): AndroidNetworkType = when (this) {
    NetworkType.NOT_REQUIRED -> AndroidNetworkType.NOT_REQUIRED
    NetworkType.CONNECTED -> AndroidNetworkType.CONNECTED
    NetworkType.UNMETERED -> AndroidNetworkType.UNMETERED
    NetworkType.NOT_ROAMING -> AndroidNetworkType.NOT_ROAMING
    NetworkType.METERED -> AndroidNetworkType.METERED
}

internal fun Constraints.toAndroid(): AndroidConstraints {
    val builder = AndroidConstraints.Builder()
        .setRequiredNetworkType(requiredNetworkType.toAndroid())
        .setRequiresCharging(requiresCharging)
        .setRequiresDeviceIdle(requiresDeviceIdle)
        .setRequiresBatteryNotLow(requiresBatteryNotLow)
        .setRequiresStorageNotLow(requiresStorageNotLow)
    contentUriTriggers.forEach { trigger ->
        builder.addContentUriTrigger(
            android.net.Uri.parse(trigger.uriString),
            trigger.triggerForDescendants,
        )
    }
    return builder.build()
}

internal fun OutOfQuotaPolicy.toAndroid(): AndroidOutOfQuotaPolicy = when (this) {
    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST -> AndroidOutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
    OutOfQuotaPolicy.DROP_WORK_REQUEST -> AndroidOutOfQuotaPolicy.DROP_WORK_REQUEST
}

internal fun ExistingPeriodicWorkPolicy.toAndroid(): AndroidExistingPeriodicWorkPolicy = when (this) {
    ExistingPeriodicWorkPolicy.KEEP -> AndroidExistingPeriodicWorkPolicy.KEEP
    ExistingPeriodicWorkPolicy.REPLACE -> AndroidExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
    ExistingPeriodicWorkPolicy.UPDATE -> AndroidExistingPeriodicWorkPolicy.UPDATE
}

internal fun AndroidWorkInfo.State.toKmp(): WorkInfo.State = when (this) {
    AndroidWorkInfo.State.ENQUEUED -> WorkInfo.State.ENQUEUED
    AndroidWorkInfo.State.RUNNING -> WorkInfo.State.RUNNING
    AndroidWorkInfo.State.SUCCEEDED -> WorkInfo.State.SUCCEEDED
    AndroidWorkInfo.State.FAILED -> WorkInfo.State.FAILED
    AndroidWorkInfo.State.BLOCKED -> WorkInfo.State.BLOCKED
    AndroidWorkInfo.State.CANCELLED -> WorkInfo.State.CANCELLED
}

internal fun AndroidWorkInfo.toKmp(): WorkInfo? {
    val id = tags.extractKmpId() ?: return null
    return WorkInfo(
        id = id,
        state = state.toKmp(),
        progress = WorkProgress(
            progress = progress.getInt("progress", 0),
        ),
        outputData = outputData.toKmp(),
        tags = tags.userTags(),
        runAttemptCount = runAttemptCount,
    )
}

internal fun Data.toKmp(): WorkData {
    val pairs = keyValueMap.entries.mapNotNull { (key, value) ->
        // Skip internal KMP keys stored in work data
        if (key == KEY_KMP_CLASS || key == KEY_KMP_ID) return@mapNotNull null
        key to value
    }
    return WorkData(*pairs.toTypedArray())
}

internal fun WorkData.toAndroid(): Data {
    val builder = Data.Builder()
    keyValueMap().forEach { (key, value) ->
        when (value) {
            is String -> builder.putString(key, value)

            is Int -> builder.putInt(key, value)

            is Long -> builder.putLong(key, value)

            is Float -> builder.putFloat(key, value)

            is Double -> builder.putDouble(key, value)

            is Boolean -> builder.putBoolean(key, value)

            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                when {
                    value.isArrayOf<String>() -> builder.putStringArray(
                        key,
                        (value as Array<String>).map {
                            it
                        }.toTypedArray(),
                    )

                    value.isArrayOf<Int>() -> builder.putIntArray(key, (value as Array<Int>).toIntArray())

                    value.isArrayOf<Long>() -> builder.putLongArray(key, (value as Array<Long>).toLongArray())
                }
            }
        }
    }
    return builder.build()
}
