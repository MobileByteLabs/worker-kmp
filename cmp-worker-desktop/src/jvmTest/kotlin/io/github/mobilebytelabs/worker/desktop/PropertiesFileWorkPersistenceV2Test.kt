package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for the [PropertiesFileWorkPersistence] schema v2 expansion delivered by
 * cross-platform-worker-parity-audit sub-plan 05 (closes G4).
 *
 * Covers:
 * - v2 payload round-trip (savePayload → loadPayload)
 * - v1 file read (legacy file without `workerClass` / `inputDataJson`) returns null payload
 *   without exception (forward-compat: no v1→v2 data loss)
 * - schemaVersion field present after first save
 */
@OptIn(ExperimentalUuidApi::class)
class PropertiesFileWorkPersistenceV2Test {

    private val tempDir: File = createTempDirectory("worker-kmp-v2-test").toFile()
    private val sut = PropertiesFileWorkPersistence(tempDir)

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun savePayload_then_loadPayload_roundTrips() = runTest {
        val id = Uuid.random()
        val payload = PersistedWorkPayload(
            workerClass = "com.example.SyncWorker",
            inputDataJson = """{"endpoint":"/api/sync"}""",
        )
        sut.savePayload(id, payload)

        val loaded = sut.loadPayload(id)

        assertNotNull(loaded)
        assertEquals("com.example.SyncWorker", loaded.workerClass)
        assertEquals("""{"endpoint":"/api/sync"}""", loaded.inputDataJson)
    }

    @Test
    fun loadPayload_absentFile_returnsNull() = runTest {
        val loaded = sut.loadPayload(Uuid.random())
        assertNull(loaded)
    }

    @Test
    fun loadPayload_v1File_withoutPayloadFields_returnsNull() = runTest {
        // Simulate a v1 .properties file by writing manually
        val id = Uuid.random()
        val file = File(tempDir, "$id.properties")
        file.writeText(
            """
            state=ENQUEUED
            runAttemptCount=0
            tags=sync,daily
            """.trimIndent(),
        )

        val loaded = sut.loadPayload(id)

        assertNull(loaded, "v1 files (no workerClass) must return null payload — caller falls back to healing-only")
    }

    @Test
    fun save_thenSavePayload_preserves_state_and_payload_in_same_file() = runTest {
        val id = Uuid.random()
        sut.save(WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, runAttemptCount = 0, tags = setOf("sync")))
        sut.savePayload(
            id,
            PersistedWorkPayload(workerClass = "com.example.SyncWorker", inputDataJson = "{}"),
        )

        // loadAll should still return the state
        val all = sut.loadAll()
        assertEquals(1, all.size)
        assertEquals(WorkInfo.State.ENQUEUED, all.first().state)

        // loadPayload should return the payload
        val payload = sut.loadPayload(id)
        assertNotNull(payload)
        assertEquals("com.example.SyncWorker", payload.workerClass)
    }

    @Test
    fun savePayload_writes_schemaVersion_field_for_v2_detection() = runTest {
        val id = Uuid.random()
        sut.savePayload(id, PersistedWorkPayload("com.example.W", "{}"))

        val file = File(tempDir, "$id.properties")
        val text = file.readText()
        assert(text.contains("schemaVersion=2")) { "schemaVersion=2 missing from persisted file:\n$text" }
    }
}
