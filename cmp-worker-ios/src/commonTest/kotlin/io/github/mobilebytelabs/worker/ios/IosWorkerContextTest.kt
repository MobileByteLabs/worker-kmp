package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class IosWorkerContextTest {

    private fun buildContext(
        id: Uuid = Uuid.random(),
        inputData: WorkData = WorkData.EMPTY,
        tags: Set<String> = emptySet(),
    ) = IosWorkerContext(
        id = id,
        inputData = inputData,
        tags = tags,
        stateStore = IosWorkStateStore(persistence = InMemoryIosWorkPersistence()),
    )

    @Test
    fun id_isReturnedFromConstructor() {
        val id = Uuid.random()
        val ctx = buildContext(id = id)
        assertEquals(id, ctx.id)
    }

    @Test
    fun inputData_isReturnedFromConstructor() {
        val data = workDataOf("k" to "v")
        val ctx = buildContext(inputData = data)
        assertEquals("v", ctx.inputData.getString("k"))
    }

    @Test
    fun tags_areReturnedFromConstructor() {
        val tags = setOf("tag1", "tag2")
        val ctx = buildContext(tags = tags)
        assertEquals(tags, ctx.tags)
    }
}
