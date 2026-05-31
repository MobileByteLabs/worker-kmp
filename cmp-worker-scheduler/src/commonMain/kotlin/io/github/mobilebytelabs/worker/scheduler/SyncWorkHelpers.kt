package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.NetworkType

const val SYNC_WORK_NAME = "SyncWork"
const val FOREGROUND_NOTIFICATION_ID_SYNC = 9_001

val SyncConstraints: Constraints = Constraints {
    setRequiredNetworkType(NetworkType.CONNECTED)
}
