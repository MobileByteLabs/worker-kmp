package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WebWorkPersistenceTest {

    @Test
    fun save_thenLoadAll_roundTrips() = runTest {
        val persistence = InMemoryWorkPersistence()
        val info = WorkInfo(id = Uuid.random(), state = WorkInfo.State.ENQUEUED, tags = setOf("daily"))
        persistence.save(info)
        val loaded = persistence.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(info.id, loaded.first().id)
        assertEquals(WorkInfo.State.ENQUEUED, loaded.first().state)
    }

    @Test
    fun save_existingId_replacesEntry() = runTest {
        val persistence = InMemoryWorkPersistence()
        val id = Uuid.random()
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = emptySet()))
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.SUCCEEDED, tags = emptySet()))
        val loaded = persistence.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(WorkInfo.State.SUCCEEDED, loaded.first().state)
    }

    @Test
    fun delete_removesEntry() = runTest {
        val persistence = InMemoryWorkPersistence()
        val id = Uuid.random()
        persistence.save(WorkInfo(id = id, state = WorkInfo.State.RUNNING, tags = emptySet()))
        persistence.delete(id)
        assertTrue(persistence.loadAll().isEmpty())
    }

    @Test
    fun loadAll_emptyStore_returnsEmptyList() = runTest {
        val persistence = InMemoryWorkPersistence()
        assertTrue(persistence.loadAll().isEmpty())
    }

    @Test
    fun seed_thenLoadAll_returnsSeeded() = runTest {
        val persistence = InMemoryWorkPersistence()
        val id = Uuid.random()
        persistence.seed(WorkInfo(id = id, state = WorkInfo.State.RUNNING, tags = setOf("seeded")))
        val loaded = persistence.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(id, loaded.first().id)
    }
}
