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

    /**
     * Schema v2 (cross-platform-worker-parity-audit sub-plan 05 — closes G4): persist enough
     * of the [WorkRequest] payload that the OS-scheduler daemon (`cmp-worker-desktop-daemon`)
     * can instantiate + run the worker directly when the consumer app is not running, instead
     * of only being able to heal stuck RUNNING → ENQUEUED transitions.
     *
     * Default impl is a no-op — implementations that don't write to disk (in-memory, no-op)
     * have nothing to persist beyond the live state map.
     */
    suspend fun savePayload(id: Uuid, payload: PersistedWorkPayload) = Unit

    /** Reads the v2 payload for [id]. Returns null if no payload was persisted (v1 file or absent). */
    suspend fun loadPayload(id: Uuid): PersistedWorkPayload? = null
}

/**
 * v2 payload for [PropertiesFileWorkPersistence]. Lets the daemon recover ENQUEUED work
 * without an active consumer app — see [DesktopWorkPersistence.savePayload] docstring.
 */
internal data class PersistedWorkPayload(
    val workerClass: String,
    val inputDataJson: String,
    // Constraints + RetryConfig serialization deliberately deferred to schema v3 follow-up —
    // current daemon impl uses sensible defaults at dispatch time when these are absent.
)

// Persists each WorkInfo as a .properties file under [directory].
// Schema v2 (closes G4): adds workerClass + inputDataJson + schemaVersion fields.
// v1 files (missing schemaVersion) are read with sensible defaults — auto-migrated to
// v2 on next save.
internal class PropertiesFileWorkPersistence(private val directory: File) : DesktopWorkPersistence {

    init {
        directory.mkdirs()
    }

    override suspend fun save(info: WorkInfo): Unit = withContext(Dispatchers.IO) {
        val file = File(directory, "${info.id}.properties")
        val props = Properties()
        // Preserve v2 payload fields if a prior savePayload was called for this id.
        if (file.exists()) {
            runCatching { file.inputStream().use { props.load(it) } }
        }
        props["schemaVersion"] = SCHEMA_VERSION_V2
        props["state"] = info.state.name
        props["runAttemptCount"] = info.runAttemptCount.toString()
        props["tags"] = info.tags.joinToString(",")
        file.outputStream().use { props.store(it, "worker-kmp persistence schema v$SCHEMA_VERSION_V2") }
    }

    override suspend fun savePayload(id: Uuid, payload: PersistedWorkPayload): Unit = withContext(Dispatchers.IO) {
        val file = File(directory, "$id.properties")
        val props = Properties()
        if (file.exists()) {
            runCatching { file.inputStream().use { props.load(it) } }
        }
        props["schemaVersion"] = SCHEMA_VERSION_V2
        props["workerClass"] = payload.workerClass
        props["inputDataJson"] = payload.inputDataJson
        file.outputStream().use { props.store(it, "worker-kmp persistence schema v$SCHEMA_VERSION_V2 (payload)") }
    }

    override suspend fun loadPayload(id: Uuid): PersistedWorkPayload? = withContext(Dispatchers.IO) {
        val file = File(directory, "$id.properties")
        if (!file.exists()) return@withContext null
        val props = Properties()
        runCatching { file.inputStream().use { props.load(it) } }.getOrNull() ?: return@withContext null
        val workerClass = props.getProperty("workerClass") ?: return@withContext null
        val inputDataJson = props.getProperty("inputDataJson") ?: "{}"
        PersistedWorkPayload(workerClass = workerClass, inputDataJson = inputDataJson)
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

    private companion object {
        const val SCHEMA_VERSION_V2 = "2"
    }
}

internal class InMemoryDesktopWorkPersistence : DesktopWorkPersistence {
    private val store = mutableListOf<WorkInfo>()
    private val payloads = mutableMapOf<Uuid, PersistedWorkPayload>()

    override suspend fun save(info: WorkInfo) {
        store.removeAll { it.id == info.id }
        store.add(info)
    }

    override suspend fun loadAll(): List<WorkInfo> = store.toList()

    override suspend fun delete(id: Uuid) {
        store.removeAll { it.id == id }
        payloads.remove(id)
    }

    override suspend fun savePayload(id: Uuid, payload: PersistedWorkPayload) {
        payloads[id] = payload
    }

    override suspend fun loadPayload(id: Uuid): PersistedWorkPayload? = payloads[id]
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
