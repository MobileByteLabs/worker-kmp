package io.github.mobilebytelabs.worker.scheduler.sync

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Tests for the v3.1.1 source-agnostic Syncable adapters: [FetcherSyncable] +
 * [CompositeSyncable].
 */
@OptIn(ExperimentalTime::class)
class SyncableAdaptersTest {

    // ─── FetcherSyncable ──────────────────────────────────────────────────────

    @Test
    fun fetcherSyncable_invokesFetcher_andBumpsVersion() = runTest {
        val sync = AdapterInMemorySynchronizer()
        var fetcherCalls = 0
        val fetcher: suspend () -> Unit = { fetcherCalls++ }
        val syncable = FetcherSyncable("currency-rates", fetcher)
        val ok = syncable.syncWith(sync)
        assertTrue(ok)
        assertEquals(1, fetcherCalls)
        assertNotNull(sync.getChangeListVersions().versions["currency-rates"])
    }

    @Test
    fun fetcherSyncable_payloadOverload_passesPayloadToFetcher() = runTest {
        val sync = AdapterInMemorySynchronizer()
        var receivedBase: String? = null
        val payloadFetcher: suspend (WorkData) -> Unit = { payload ->
            receivedBase = payload.getString("currency.base")
        }
        val syncable = FetcherSyncable("currency-rates", payloadFetcher)
        syncable.syncWith(sync, workDataOf("currency.base" to "EUR"))
        assertEquals("EUR", receivedBase)
    }

    @Test
    fun fetcherSyncable_noArgConstructor_delegatesWithEmptyPayload() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val plainFetcher: suspend () -> Unit = { /* no payload param */ }
        val syncable = FetcherSyncable("x", plainFetcher)
        assertTrue(syncable.syncWith(sync))
    }

    @Test
    fun fetcherSyncable_fetcherThrows_returnsFalse_andDoesNotBumpVersion() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val throwingFetcher: suspend () -> Unit = { error("network down") }
        val syncable = FetcherSyncable("currency-rates", throwingFetcher)
        val ok = syncable.syncWith(sync)
        assertFalse(ok)
        // snapshotSync's runCatching{}.getOrElse { false } catches → no version bump
        assertEquals(null, sync.getChangeListVersions().versions["currency-rates"])
    }

    @Test
    fun fetcherSyncable_multipleSyncables_distinctNames_independentVersionRows() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val noop: suspend () -> Unit = { /* noop */ }
        FetcherSyncable("a", noop).syncWith(sync)
        FetcherSyncable("b", noop).syncWith(sync)
        FetcherSyncable("c", noop).syncWith(sync)
        val versions = sync.getChangeListVersions().versions
        assertNotNull(versions["a"])
        assertNotNull(versions["b"])
        assertNotNull(versions["c"])
    }

    // ─── CompositeSyncable ────────────────────────────────────────────────────

    @Test
    fun compositeSyncable_allChildrenTrue_returnsTrue() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val composite = CompositeSyncable(AlwaysTrue(), AlwaysTrue(), AlwaysTrue())
        assertTrue(composite.syncWith(sync))
    }

    @Test
    fun compositeSyncable_anyChildFalse_returnsFalse_andShortCircuits() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val third = AdapterRecordingSyncable(returns = true)
        val composite = CompositeSyncable(AlwaysTrue(), AlwaysFalse(), third)
        val ok = composite.syncWith(sync)
        assertFalse(ok)
        // Short-circuits — third syncable is never called because second returned false
        assertEquals(0, third.callCount)
    }

    @Test
    fun compositeSyncable_runsChildrenInDeclarationOrder() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val order = mutableListOf<String>()
        val composite = CompositeSyncable(
            OrderRecording("first", order),
            OrderRecording("second", order),
            OrderRecording("third", order),
        )
        composite.syncWith(sync)
        assertEquals(listOf("first", "second", "third"), order)
    }

    @Test
    fun compositeSyncable_payloadOverload_propagatesPayload() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val a = AdapterRecordingSyncable(returns = true)
        val b = AdapterRecordingSyncable(returns = true)
        val composite = CompositeSyncable(a, b)
        composite.syncWith(sync, workDataOf("k" to "v"))
        assertEquals("v", a.lastPayload?.getString("k"))
        assertEquals("v", b.lastPayload?.getString("k"))
    }

    @Test
    fun compositeSyncable_varargAndListConstructors_equivalent() = runTest {
        val sync = AdapterInMemorySynchronizer()
        val varargForm = CompositeSyncable(AlwaysTrue(), AlwaysTrue())
        val listForm = CompositeSyncable(listOf(AlwaysTrue(), AlwaysTrue()))
        assertTrue(varargForm.syncWith(sync))
        assertTrue(listForm.syncWith(sync))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test fakes
// ──────────────────────────────────────────────────────────────────────────────

private class AdapterInMemorySynchronizer : Synchronizer {
    private var state = ChangeListVersions()
    override suspend fun getChangeListVersions() = state
    override suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions) {
        state = state.update()
    }
}

private class AlwaysTrue : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer) = true
}

private class AlwaysFalse : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer) = false
}

private class AdapterRecordingSyncable(private val returns: Boolean) : Syncable {
    var callCount = 0
    var lastPayload: WorkData? = null
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        callCount++
        return returns
    }
    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean {
        callCount++
        lastPayload = payload
        return returns
    }
}

private class OrderRecording(private val name: String, private val order: MutableList<String>) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        order += name
        return true
    }
}
