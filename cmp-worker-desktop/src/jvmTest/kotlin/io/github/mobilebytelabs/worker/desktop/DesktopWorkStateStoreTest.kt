package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class DesktopWorkStateStoreTest {

    @Test
    fun initWork_surfacesEnqueuedState() = runTest {
        val store = DesktopWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, emptySet())
        assertEquals(WorkInfo.State.ENQUEUED, store.getById(id)?.state)
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        val store = DesktopWorkStateStore()
        assertNull(store.getById(Uuid.random()))
    }

    @Test
    fun updateState_toSucceeded_reflectsInSnapshot() = runTest {
        val store = DesktopWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, emptySet())
        store.updateState(id, WorkInfo.State.SUCCEEDED)
        assertEquals(WorkInfo.State.SUCCEEDED, store.getById(id)?.state)
    }

    @Test
    fun observeByTag_emitsOnInit() = runTest {
        val store = DesktopWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, setOf("data-sync"))
        val items = store.observeByTag("data-sync").first()
        assertEquals(1, items.size)
        assertEquals(id, items.first().id)
    }
}
