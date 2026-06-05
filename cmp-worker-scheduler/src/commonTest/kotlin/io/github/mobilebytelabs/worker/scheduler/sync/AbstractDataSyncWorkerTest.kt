package io.github.mobilebytelabs.worker.scheduler.sync

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for [AbstractDataSyncWorker]'s parallel fan-out + WorkResult mapping semantics.
 *
 * Persister assertions go via [SyncStatePersister.read] after `doWork()` — the persister
 * is `final` so we can't intercept writes via subclass; instead we use its actual
 * MutableStateFlow-backed behavior (read returns the latest write).
 */
class AbstractDataSyncWorkerTest {

    // ─── Result mapping ───────────────────────────────────────────────────────

    @Test
    fun doWork_allSyncablesTrue_returnsSuccess_andPersistsBumpedVersions() = runTest {
        val persister = SyncStatePersister()
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = listOf(
                VersionBumpingSyncable("currency", finalVersion = 100L),
                VersionBumpingSyncable("macro", finalVersion = 42L),
            ),
            persister = persister,
        )
        val result = worker.doWork()
        assertEquals(WorkResult.Success(), result)
        val written = persister.read()
        // On success, the worker writes its accumulated ChangeListVersions back to the persister
        assertEquals(100L, written.versions["currency"])
        assertEquals(42L, written.versions["macro"])
    }

    @Test
    fun doWork_anySyncableFalse_returnsRetry_andDoesNotPersist() = runTest {
        val persister = SyncStatePersister()
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = listOf(
                VersionBumpingSyncable("currency", finalVersion = 100L),
                AlwaysFalseSyncable(),
            ),
            persister = persister,
        )
        val result = worker.doWork()
        assertTrue(result is WorkResult.Retry)
        // No write — persister's read returns the empty initial state
        assertNull(persister.read().versions["currency"])
    }

    @Test
    fun doWork_syncableThrows_returnsFailure_withMessage() = runTest {
        val persister = SyncStatePersister()
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = listOf(ThrowingSyncable("currency API exploded")),
            persister = persister,
        )
        val result = worker.doWork()
        assertTrue(result is WorkResult.Failure)
        assertEquals("currency API exploded", (result as WorkResult.Failure).message)
        assertNull(persister.read().versions["currency"])
    }

    @Test
    fun doWork_syncableThrowsWithNullMessage_failureUsesClassName() = runTest {
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = listOf(ThrowingSyncable(message = null, exception = SyntheticError())),
            persister = SyncStatePersister(),
        )
        val result = worker.doWork() as WorkResult.Failure
        // Falls back to class simpleName when t.message is null
        assertEquals("SyntheticError", result.message)
    }

    // ─── Fan-out / parallelism ────────────────────────────────────────────────

    @Test
    fun doWork_invokesEachSyncable_withInputDataPayload() = runTest {
        val recordingA = RecordingSyncable()
        val recordingB = RecordingSyncable()
        val payload = workDataOf("currency.base" to "USD", "loan.id" to "L42")
        val worker = TestSyncWorker(
            ctx = fakeCtx(inputData = payload),
            syncables = listOf(recordingA, recordingB),
            persister = SyncStatePersister(),
        )
        worker.doWork()
        assertEquals(1, recordingA.callCount)
        assertEquals(1, recordingB.callCount)
        // Payload threads to the 2-arg syncWith overload
        assertEquals("USD", recordingA.lastPayload?.getString("currency.base"))
        assertEquals("L42", recordingB.lastPayload?.getString("loan.id"))
    }

    @Test
    fun doWork_runsSyncablesInParallel_notSequentially() = runTest {
        val gateA = CompletableDeferred<Unit>()
        val gateB = CompletableDeferred<Unit>()
        // A waits for B to signal; B waits for A to signal. If runs sequentially, deadlocks.
        // If runs in parallel via async + awaitAll, both gates complete + both syncables return.
        val a = SyncableWithGate(thisGate = gateA, otherGate = gateB)
        val b = SyncableWithGate(thisGate = gateB, otherGate = gateA)
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = listOf(a, b),
            persister = SyncStatePersister(),
        )
        val result = worker.doWork()
        // If sequential, the deadlock would hang runTest until its scheduler timeout.
        // Reaching here means awaitAll dispatched both syncWith calls concurrently.
        assertEquals(WorkResult.Success(), result)
    }

    @Test
    fun doWork_emptySyncablesList_isSuccess_noPersistChange() = runTest {
        val persister = SyncStatePersister()
        val worker = TestSyncWorker(
            ctx = fakeCtx(),
            syncables = emptyList(),
            persister = persister,
        )
        val result = worker.doWork()
        // All-true on empty list → success
        assertEquals(WorkResult.Success(), result)
        // Persister still writes the (empty) ChangeListVersions
        assertNotNull(persister.read())
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test fakes
// ──────────────────────────────────────────────────────────────────────────────

private class TestSyncWorker(ctx: WorkerContext, syncables: List<Syncable>, persister: SyncStatePersister) :
    AbstractDataSyncWorker(ctx, syncables, persister)

@OptIn(ExperimentalUuidApi::class)
private fun fakeCtx(inputData: WorkData = workDataOf()): WorkerContext = object : WorkerContext {
    override val id: Uuid = Uuid.random()
    override val inputData: WorkData = inputData
    override val tags: Set<String> = emptySet()
    override suspend fun setProgress(progress: io.github.mobilebytelabs.worker.WorkProgress) = Unit
}

private class AlwaysFalseSyncable : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer) = false
}

private class ThrowingSyncable(
    private val message: String?,
    private val exception: Throwable = RuntimeException(message),
) : Syncable {
    constructor(message: String) : this(message, RuntimeException(message))
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean = throw exception
}

private class SyntheticError : Throwable(message = null)

private class RecordingSyncable : Syncable {
    var callCount = 0
    var lastPayload: WorkData? = null
    override suspend fun syncWith(synchronizer: Synchronizer) = true
    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean {
        callCount++
        lastPayload = payload
        return true
    }
}

private class SyncableWithGate(
    private val thisGate: CompletableDeferred<Unit>,
    private val otherGate: CompletableDeferred<Unit>,
) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        thisGate.complete(Unit)
        otherGate.await()
        return true
    }
}

private class VersionBumpingSyncable(private val name: String, private val finalVersion: Long) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        synchronizer.updateChangeListVersions { copy(versions = versions + (name to finalVersion)) }
        return true
    }
}

// ─── ChangeListVersions unit tests ───────────────────────────────────────────

class ChangeListVersionsTest {

    @Test
    fun set_addsNewKey_returnsUpdatedCopy() {
        val empty = ChangeListVersions()
        val updated = empty.set("currency", 42L)
        assertEquals(42L, updated.versions["currency"])
        assertTrue(empty.versions.isEmpty(), "set() must not mutate the receiver")
    }

    @Test
    fun set_updatesExistingKey_returnsUpdatedCopy() {
        val initial = ChangeListVersions(mapOf("currency" to 1L))
        val updated = initial.set("currency", 99L)
        assertEquals(99L, updated.versions["currency"])
        assertEquals(1L, initial.versions["currency"], "Original must be unchanged")
    }
}

// ─── getChangeListVersions direct tests ──────────────────────────────────────

class GetChangeListVersionsTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun getChangeListVersions_returnsCurrentWorkingVersions() = runTest {
        val persister = SyncStatePersister()
        val worker = TestSyncWorker(
            ctx = object : WorkerContext {
                override val id = Uuid.random()
                override val inputData = workDataOf()
                override val tags: Set<String> = emptySet()
                override suspend fun setProgress(progress: io.github.mobilebytelabs.worker.WorkProgress) = Unit
            },
            syncables = emptyList(),
            persister = persister,
        )
        // Initial versions are empty; getChangeListVersions() delegates to workingVersions
        val versions = worker.getChangeListVersions()
        assertEquals(emptyMap(), versions.versions)
    }
}
