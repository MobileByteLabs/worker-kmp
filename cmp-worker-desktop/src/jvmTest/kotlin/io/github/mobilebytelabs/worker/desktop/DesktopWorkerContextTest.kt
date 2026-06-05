package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DesktopWorkerContextTest {

    private fun buildContext(
        id: Uuid = Uuid.random(),
        inputData: WorkData = WorkData.EMPTY,
        tags: Set<String> = emptySet(),
    ) = DesktopWorkerContext(
        id = id,
        inputData = inputData,
        tags = tags,
        stateStore = DesktopWorkStateStore(),
    )

    @Test
    fun id_isReturnedFromConstructor() {
        val id = Uuid.random()
        assertEquals(id, buildContext(id = id).id)
    }

    @Test
    fun inputData_isReturnedFromConstructor() {
        val data = workDataOf("k" to "v")
        assertEquals("v", buildContext(inputData = data).inputData.getString("k"))
    }

    @Test
    fun tags_areReturnedFromConstructor() {
        val tags = setOf("tag1", "tag2")
        assertEquals(tags, buildContext(tags = tags).tags)
    }
}
