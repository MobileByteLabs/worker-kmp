package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class IosWorkPersistenceTest {

    private fun persistence() = InMemoryIosWorkPersistence()

    private fun workInfo(state: WorkInfo.State = WorkInfo.State.ENQUEUED) = WorkInfo(
        id = Uuid.random(),
        state = state,
        tags = emptySet(),
    )

    @Test
    fun save_and_loadAll_roundTrips() = runTest {
        val p = persistence()
        val info = workInfo()
        p.save(info)
        val loaded = p.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(info.id, loaded.first().id)
    }

    @Test
    fun delete_removesItem() = runTest {
        val p = persistence()
        val info = workInfo()
        p.save(info)
        p.delete(info.id)
        assertTrue(p.loadAll().isEmpty())
    }

    @Test
    fun save_overwrites_existingEntry() = runTest {
        val p = persistence()
        val info = workInfo(WorkInfo.State.ENQUEUED)
        p.save(info)
        val updated = info.copy(state = WorkInfo.State.RUNNING)
        p.save(updated)
        val loaded = p.loadAll()
        assertEquals(1, loaded.size)
        assertEquals(WorkInfo.State.RUNNING, loaded.first().state)
    }
}
