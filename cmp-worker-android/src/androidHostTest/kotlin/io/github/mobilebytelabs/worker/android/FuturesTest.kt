package io.github.mobilebytelabs.worker.android

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FuturesTest {
    @Test
    fun await_returnsValueFromImmediateFuture() = runTest {
        val future = Futures.immediateFuture("hello")
        val result = future.await()
        assertEquals("hello", result)
    }

    @Test
    fun await_throwsOnFailedFuture() = runTest {
        val ex = RuntimeException("test failure")
        val future = Futures.immediateFailedFuture<String>(ex)
        // ListenableFuture.await() propagates the cause (RuntimeException) or wraps in ExecutionException
        val thrown = runCatching { future.await() }.exceptionOrNull()
        assertNotNull(thrown, "failed future must throw on await")
    }
}
