package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkInfo
import kotlin.uuid.Uuid

private object NoOpWorkPersistence : WebWorkPersistence {
    override suspend fun save(info: WorkInfo) = Unit
    override suspend fun loadAll(): List<WorkInfo> = emptyList()
    override suspend fun delete(id: Uuid) = Unit
}

internal actual fun createWebWorkPersistence(config: WebWorkManagerConfig): WebWorkPersistence = NoOpWorkPersistence
