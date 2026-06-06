package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class IosWorkStateStoreTest {

    private fun store() = IosWorkStateStore(persistence = InMemoryIosWorkPersistence())

    @Test
    fun initWork_surfacesEnqueuedState() = runTest {
        val s = store()
        val id = Uuid.random()
        s.initWork(id, emptySet())
        val info = s.getById(id)
        assertEquals(WorkInfo.State.ENQUEUED, info?.state)
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        val s = store()
        assertNull(s.getById(Uuid.random()))
    }

    @Test
    fun updateState_toSucceeded_reflectsInSnapshot() = runTest {
        val s = store()
        val id = Uuid.random()
        s.initWork(id, emptySet())
        s.updateState(id, WorkInfo.State.SUCCEEDED)
        val info = s.getById(id)
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
    }
}
