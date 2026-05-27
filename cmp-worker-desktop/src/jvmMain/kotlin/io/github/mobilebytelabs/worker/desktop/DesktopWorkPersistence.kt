package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import kotlin.uuid.Uuid

internal interface DesktopWorkPersistence {
    suspend fun save(info: WorkInfo)
    suspend fun loadAll(): List<WorkInfo>
    suspend fun delete(id: Uuid)
}

// Persists each WorkInfo as a .properties file under [directory].
// File name is the work ID; fields: state, runAttemptCount, tags (comma-separated).
internal class PropertiesFileWorkPersistence(private val directory: File) : DesktopWorkPersistence {

    init {
        directory.mkdirs()
    }

    override suspend fun save(info: WorkInfo): Unit = withContext(Dispatchers.IO) {
        val props = Properties()
        props["state"] = info.state.name
        props["runAttemptCount"] = info.runAttemptCount.toString()
        props["tags"] = info.tags.joinToString(",")
        File(directory, "${info.id}.properties").outputStream().use { props.store(it, null) }
    }

    override suspend fun loadAll(): List<WorkInfo> = withContext(Dispatchers.IO) {
        directory.listFiles { f -> f.extension == "properties" }
            ?.mapNotNull { file ->
                runCatching {
                    val props = Properties()
                    file.inputStream().use { props.load(it) }
                    val id = Uuid.parse(file.nameWithoutExtension)
                    val state = WorkInfo.State.valueOf(props.getProperty("state"))
                    val runAttemptCount = props.getProperty("runAttemptCount")?.toIntOrNull() ?: 0
                    val tags = props.getProperty("tags")
                        ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                    WorkInfo(id = id, state = state, runAttemptCount = runAttemptCount, tags = tags)
                }.getOrNull()
            } ?: emptyList()
    }

    override suspend fun delete(id: Uuid): Unit = withContext(Dispatchers.IO) {
        File(directory, "$id.properties").delete()
    }
}

internal class InMemoryDesktopWorkPersistence : DesktopWorkPersistence {
    private val store = mutableListOf<WorkInfo>()

    override suspend fun save(info: WorkInfo) {
        store.removeAll { it.id == info.id }
        store.add(info)
    }

    override suspend fun loadAll(): List<WorkInfo> = store.toList()

    override suspend fun delete(id: Uuid) {
        store.removeAll { it.id == id }
    }
}

internal object NoOpDesktopWorkPersistence : DesktopWorkPersistence {
    override suspend fun save(info: WorkInfo) = Unit
    override suspend fun loadAll(): List<WorkInfo> = emptyList()
    override suspend fun delete(id: Uuid) = Unit
}

internal fun createDesktopWorkPersistence(config: DesktopWorkManagerConfig): DesktopWorkPersistence =
    if (config.persistenceEnabled) {
        PropertiesFileWorkPersistence(config.persistencePath)
    } else {
        NoOpDesktopWorkPersistence
    }
