package io.github.mobilebytelabs.worker.daemon

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DaemonWorkerFactoryTest {

    @Test
    fun daemonWorkResult_success_isDistinctType() {
        val result: DaemonWorkResult = DaemonWorkResult.Success
        assertTrue(result is DaemonWorkResult.Success)
    }

    @Test
    fun daemonWorkResult_failure_holdsReason() {
        val result = DaemonWorkResult.Failure("network timeout")
        assertEquals("network timeout", result.reason)
        assertTrue(result is DaemonWorkResult.Failure)
    }

    @Test
    fun daemonWorkResult_retry_holdsReason() {
        val result = DaemonWorkResult.Retry("rate limit exceeded")
        assertEquals("rate limit exceeded", result.reason)
        assertTrue(result is DaemonWorkResult.Retry)
    }

    @Test
    fun daemonWorkResult_sealedTypes_areExhaustive() {
        val results: List<DaemonWorkResult> = listOf(
            DaemonWorkResult.Success,
            DaemonWorkResult.Failure("f"),
            DaemonWorkResult.Retry("r"),
        )
        assertEquals(3, results.size)
        assertTrue(results.all { it is DaemonWorkResult })
    }

    @Test
    fun daemonWorkerDispatch_noFactoriesRegistered_returnsNull() = runBlocking {
        val result = DaemonWorkerDispatch.dispatch(
            workerClass = "com.example.MyWorker",
            inputDataJson = "{}",
        )
        assertNull(result, "dispatch with no ServiceLoader factories should return null")
    }

    @Test
    fun daemonWorkerFactory_anonymousImpl_createReturnsResult() = runBlocking {
        val factory = object : DaemonWorkerFactory {
            override suspend fun create(workerClass: String, inputDataJson: String): DaemonWorkResult? =
                if (workerClass == "com.example.TestWorker") DaemonWorkResult.Success else null
        }
        val result = factory.create("com.example.TestWorker", "{}")
        assertNotNull(result)
        assertTrue(result is DaemonWorkResult.Success)
    }

    @Test
    fun daemonWorkerFactory_anonymousImpl_unknownClass_returnsNull() = runBlocking {
        val factory = object : DaemonWorkerFactory {
            override suspend fun create(workerClass: String, inputDataJson: String): DaemonWorkResult? = null
        }
        assertNull(factory.create("com.example.Unknown", "{}"))
    }
}
