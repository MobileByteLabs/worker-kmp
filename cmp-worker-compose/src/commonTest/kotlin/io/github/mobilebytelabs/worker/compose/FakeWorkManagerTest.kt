package io.github.mobilebytelabs.worker.compose

import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class FakeWorkManagerTest {

    private val manager = FakeWorkManager()

    @Test
    fun getWorkInfosByTag_emptyInitially() = runTest {
        val result = manager.getWorkInfosByTag("any").first()
        assertEquals(emptyList(), result)
    }

    @Test
    fun getWorkInfosByTag_emitsAddedInfos() = runTest {
        val tag = "sync"
        val info = WorkInfo(id = Uuid.random(), state = WorkInfo.State.RUNNING)

        manager.putInfos(tag, info)

        val result = manager.getWorkInfosByTag(tag).first()
        assertEquals(listOf(info), result)
    }

    @Test
    fun getWorkInfosByTag_isolatedByTag() = runTest {
        val id1 = Uuid.random()
        val id2 = Uuid.random()
        manager.putInfos("alpha", WorkInfo(id = id1, state = WorkInfo.State.ENQUEUED))
        manager.putInfos("beta", WorkInfo(id = id2, state = WorkInfo.State.SUCCEEDED))

        val alpha = manager.getWorkInfosByTag("alpha").first()
        val beta = manager.getWorkInfosByTag("beta").first()

        assertEquals(1, alpha.size)
        assertEquals(id1, alpha[0].id)
        assertEquals(1, beta.size)
        assertEquals(id2, beta[0].id)
    }

    @Test
    fun getWorkInfosByTag_reflectsSequentialUpdates() = runTest {
        val tag = "upload"

        assertEquals(emptyList(), manager.getWorkInfosByTag(tag).first())

        val id1 = Uuid.random()
        manager.putInfos(tag, WorkInfo(id = id1, state = WorkInfo.State.ENQUEUED))
        val after1 = manager.getWorkInfosByTag(tag).first()
        assertEquals(1, after1.size)
        assertEquals(WorkInfo.State.ENQUEUED, after1[0].state)

        val id2 = Uuid.random()
        manager.putInfos(tag, WorkInfo(id = id2, state = WorkInfo.State.RUNNING))
        val after2 = manager.getWorkInfosByTag(tag).first()
        assertEquals(1, after2.size)
        assertEquals(WorkInfo.State.RUNNING, after2[0].state)
    }

    @Test
    fun getWorkInfoById_returnsNullForUnknown() = runTest {
        assertNull(manager.getWorkInfoById(Uuid.random()))
    }

    @Test
    fun getWorkInfoById_findsById() = runTest {
        val id = Uuid.random()
        val info = WorkInfo(
            id = id,
            state = WorkInfo.State.SUCCEEDED,
            progress = WorkProgress(100)
        )
        manager.putInfos("tag", info)

        val found = manager.getWorkInfoById(id)
        assertEquals(info, found)
    }

    @Test
    fun enqueue_recordsRequest() = runTest {
        val req = io.github.mobilebytelabs.worker.oneTimeWorkRequest<DummyWorker>()
        manager.enqueue(req)

        assertEquals(1, manager.enqueuedRequests.size)
        assertEquals(req.id, manager.enqueuedRequests[0].id)
    }

    @Test
    fun cancelWorkById_recordsId() = runTest {
        val id = Uuid.random()
        manager.cancelWorkById(id)

        assertEquals(listOf(id), manager.cancelledIds)
    }

    @Test
    fun cancelAllWorkByTag_recordsTag() = runTest {
        manager.cancelAllWorkByTag("sync")
        manager.cancelAllWorkByTag("upload")

        assertEquals(listOf("sync", "upload"), manager.cancelledTags)
    }
}
