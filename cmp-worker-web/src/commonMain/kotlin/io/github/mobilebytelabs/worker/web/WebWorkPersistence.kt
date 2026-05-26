package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkInfo
import kotlin.uuid.Uuid

internal interface WebWorkPersistence {
    suspend fun save(info: WorkInfo)
    suspend fun loadAll(): List<WorkInfo>
    suspend fun delete(id: Uuid)
}

internal expect fun createWebWorkPersistence(config: WebWorkManagerConfig): WebWorkPersistence
