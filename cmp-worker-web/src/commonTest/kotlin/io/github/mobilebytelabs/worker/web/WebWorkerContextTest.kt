package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class WebWorkerContextTest {

    @Test
    fun id_roundTrips() {
        val id = Uuid.random()
        val ctx =
            WebWorkerContext(id = id, inputData = WorkData.EMPTY, tags = emptySet(), stateStore = WebWorkStateStore())
        assertEquals(id, ctx.id)
    }

    @Test
    fun inputData_roundTrips() {
        val data = WorkData("key" to "value")
        val ctx = WebWorkerContext(
            id = Uuid.random(),
            inputData = data,
            tags = emptySet(),
            stateStore = WebWorkStateStore(),
        )
        assertEquals("value", ctx.inputData.getString("key"))
    }

    @Test
    fun tags_roundTrips() {
        val tags = setOf("sync", "daily")
        val ctx = WebWorkerContext(
            id = Uuid.random(),
            inputData = WorkData.EMPTY,
            tags = tags,
            stateStore = WebWorkStateStore(),
        )
        assertEquals(tags, ctx.tags)
    }

    @Test
    fun setProgress_doesNotThrow() = runTest {
        val store = WebWorkStateStore()
        val id = Uuid.random()
        store.initWork(id, emptySet())
        val ctx = WebWorkerContext(id = id, inputData = WorkData.EMPTY, tags = emptySet(), stateStore = store)
        ctx.setProgress(WorkProgress(50))
    }
}
