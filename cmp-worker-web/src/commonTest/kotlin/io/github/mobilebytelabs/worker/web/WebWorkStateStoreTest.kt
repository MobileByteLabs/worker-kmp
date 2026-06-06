package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WebWorkStateStoreTest {

    @Test
    fun initWork_thenGetById_returnsEnqueued() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, setOf("sync"))
        val info = store.getById(id)
        assertNotNull(info)
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        val store = WebWorkStateStore()
        assertNull(store.getById(Uuid.random()))
    }

    @Test
    fun updateState_transitionsState() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, emptySet())
        store.updateState(id, WorkInfo.State.SUCCEEDED)
        assertEquals(WorkInfo.State.SUCCEEDED, store.getById(id)?.state)
    }

    @Test
    fun observeByTag_emitsOnInit() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, setOf("data-sync"))
        val items = store.observeByTag("data-sync").first()
        assertEquals(1, items.size)
        assertEquals(id, items.first().id)
    }

    @Test
    fun transitionToRunning_fromEnqueued_returnsTrue() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, emptySet())
        val result = store.transitionToRunning(id)
        assertTrue(result, "transition from ENQUEUED to RUNNING should succeed")
        assertEquals(WorkInfo.State.RUNNING, store.getById(id)?.state)
    }

    @Test
    fun transitionToRunning_unknownId_returnsFalse() = runTest {
        val store = WebWorkStateStore()
        val result = store.transitionToRunning(Uuid.random())
        assertTrue(!result, "transition for unknown id should fail")
    }

    @Test
    fun snapshot_returnsCurrentState() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, setOf("tag1"))
        val snapshot = store.snapshot()
        assertNotNull(snapshot[id])
        assertEquals(WorkInfo.State.ENQUEUED, snapshot[id]?.state)
    }
}
