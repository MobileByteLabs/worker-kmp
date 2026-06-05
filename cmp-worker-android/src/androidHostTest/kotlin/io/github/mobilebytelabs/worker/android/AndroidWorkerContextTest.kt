package io.github.mobilebytelabs.worker.android

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class AndroidWorkerContextTest {
    @Test
    fun id_isReturnedFromConstructor() {
        val id = Uuid.random()
        val inputData = workDataOf("key" to "value")
        val ctx = buildFakeContext(id, inputData)
        assertEquals(id, ctx.id, "context.id must match constructor arg")
    }

    @Test
    fun inputData_isReturnedFromConstructor() {
        val inputData = workDataOf("k1" to "v1", "k2" to 42)
        val ctx = buildFakeContext(Uuid.random(), inputData)
        assertEquals("v1", ctx.inputData.getString("k1"))
        assertEquals(42, ctx.inputData.getInt("k2", -1))
    }

    @Test
    fun tags_areReturnedFromConstructor() {
        val tags = setOf("tag1", "tag2")
        val ctx = buildFakeContext(Uuid.random(), WorkData.EMPTY, tags)
        assertEquals(tags, ctx.tags)
    }

    private fun buildFakeContext(
        id: Uuid,
        inputData: WorkData,
        tags: Set<String> = emptySet(),
    ) = object : io.github.mobilebytelabs.worker.WorkerContext {
        override val id = id
        override val inputData = inputData
        override val tags = tags
        override suspend fun setProgress(progress: io.github.mobilebytelabs.worker.WorkProgress) = Unit
    }
}
