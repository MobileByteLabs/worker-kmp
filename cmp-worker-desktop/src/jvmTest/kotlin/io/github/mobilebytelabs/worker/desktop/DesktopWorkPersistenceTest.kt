package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class DesktopWorkPersistenceTest {
    private val tempDir: File = Files.createTempDirectory("desktop-persistence-test").toFile()

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun save_thenLoadAll_roundTrips() = runTest {
        val persistence = PropertiesFileWorkPersistence(tempDir)
        val info = WorkInfo(id = Uuid.random(), state = WorkInfo.State.ENQUEUED, tags = setOf("daily-sync"))
        persistence.save(info)
        val loaded = persistence.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(info.id, loaded.first().id)
        assertEquals(info.state, loaded.first().state)
    }

    @Test
    fun save_existingId_replacesInPlace() = runTest {
        val persistence = PropertiesFileWorkPersistence(tempDir)
        val id = Uuid.random()
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = emptySet()))
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.SUCCEEDED, tags = emptySet()))
        val loaded = persistence.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(WorkInfo.State.SUCCEEDED, loaded.first().state)
    }

    @Test
    fun save_persistsToDisk() = runTest {
        val persistence = PropertiesFileWorkPersistence(tempDir)
        persistence.save(WorkInfo(id = Uuid.random(), state = WorkInfo.State.RUNNING, tags = emptySet()))
        assertTrue(tempDir.listFiles()!!.isNotEmpty(), "save must create on-disk artifact")
    }

    @Test
    fun delete_removesEntry() = runTest {
        val persistence = PropertiesFileWorkPersistence(tempDir)
        val id = Uuid.random()
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = emptySet()))
        persistence.delete(id)
        assertTrue(persistence.loadAll().isEmpty())
    }
}
