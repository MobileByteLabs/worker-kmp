package io.github.mobilebytelabs.worker.storeflow.paging

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Covers all branches of [PrefetchPagingWorker.doWork] — success path, retry-on-failure,
 * default page count fallback (negative / unset / zero), companion-object constants.
 */
class PrefetchPagingWorkerTest {

    private class TestContext(override val inputData: WorkData) : WorkerContext {
        override val id: Uuid = Uuid.random()
        override val tags: Set<String> = emptySet()
        override suspend fun setProgress(progress: WorkProgress) = Unit
    }

    private class RecordingWorker(context: WorkerContext, private val failOnPage: Int? = null) :
        PrefetchPagingWorker(context) {
        val fetched: MutableList<Int> = mutableListOf()
        override suspend fun fetchPage(pageNumber: Int) {
            if (pageNumber == failOnPage) error("boom page=$pageNumber")
            fetched += pageNumber
        }
    }

    @Test
    fun doWork_fetchesAllPagesSuccessfully() = runTest {
        val ctx = TestContext(
            workDataOf(
                PrefetchPagingWorker.KEY_START_PAGE to 5,
                PrefetchPagingWorker.KEY_PAGE_COUNT to 3,
            ),
        )
        val worker = RecordingWorker(ctx)
        val result = worker.doWork()
        assertIs<WorkResult.Success>(result)
        assertEquals(listOf(5, 6, 7), worker.fetched)
        assertEquals(3, result.outputData.getInt(PrefetchPagingWorker.KEY_PAGES_FETCHED))
    }

    @Test
    fun doWork_retryOnFailure_capturesPageInMessage() = runTest {
        val ctx = TestContext(
            workDataOf(
                PrefetchPagingWorker.KEY_START_PAGE to 1,
                PrefetchPagingWorker.KEY_PAGE_COUNT to 5,
            ),
        )
        val worker = RecordingWorker(ctx, failOnPage = 3)
        val result = worker.doWork()
        assertIs<WorkResult.Retry>(result)
        assertTrue(result.reason.isNotEmpty())
        assertEquals(listOf(1, 2), worker.fetched)
    }

    @Test
    fun doWork_zeroPageCount_fallsBackToDefault() = runTest {
        val ctx = TestContext(
            workDataOf(
                PrefetchPagingWorker.KEY_START_PAGE to 10,
                PrefetchPagingWorker.KEY_PAGE_COUNT to 0,
            ),
        )
        val worker = RecordingWorker(ctx)
        val result = worker.doWork()
        assertIs<WorkResult.Success>(result)
        // DEFAULT_PAGE_COUNT = 3 → pages 10, 11, 12.
        assertEquals(listOf(10, 11, 12), worker.fetched)
        assertEquals(PrefetchPagingWorker.DEFAULT_PAGE_COUNT, worker.fetched.size)
    }

    @Test
    fun doWork_negativePageCount_fallsBackToDefault() = runTest {
        val ctx = TestContext(
            workDataOf(
                PrefetchPagingWorker.KEY_START_PAGE to 0,
                PrefetchPagingWorker.KEY_PAGE_COUNT to -5,
            ),
        )
        val worker = RecordingWorker(ctx)
        worker.doWork()
        assertEquals(PrefetchPagingWorker.DEFAULT_PAGE_COUNT, worker.fetched.size)
    }

    @Test
    fun doWork_unsetPageCount_usesDefault() = runTest {
        val ctx = TestContext(workDataOf(PrefetchPagingWorker.KEY_START_PAGE to 2))
        val worker = RecordingWorker(ctx)
        val result = worker.doWork()
        assertIs<WorkResult.Success>(result)
        assertEquals(PrefetchPagingWorker.DEFAULT_PAGE_COUNT, worker.fetched.size)
        assertEquals(listOf(2, 3, 4), worker.fetched)
    }

    @Test
    fun companion_constants() {
        assertEquals("prefetch.startPage", PrefetchPagingWorker.KEY_START_PAGE)
        assertEquals("prefetch.pageCount", PrefetchPagingWorker.KEY_PAGE_COUNT)
        assertEquals("prefetch.pagesFetched", PrefetchPagingWorker.KEY_PAGES_FETCHED)
        assertEquals(3, PrefetchPagingWorker.DEFAULT_PAGE_COUNT)
    }
}
