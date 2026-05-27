package io.github.mobilebytelabs.worker.storeflow

import io.github.mobilebytelabs.worker.storeflow.policy.FetchPolicy
import io.github.mobilebytelabs.worker.storeflow.submit.InMemorySubmitOutbox
import io.github.mobilebytelabs.worker.storeflow.submit.OutboxState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoreFlowSmokeTest {

    @Test
    fun fetchPolicy_hasThreeVariants() {
        assertEquals(3, FetchPolicy.entries.size)
        assertNotNull(FetchPolicy.CACHE_THEN_NETWORK)
        assertNotNull(FetchPolicy.NETWORK_ONLY)
        assertNotNull(FetchPolicy.CACHE_ONLY)
    }

    @Test
    fun inMemoryOutbox_enqueueAndQuery() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        outbox.enqueue("payload-1")
        outbox.enqueue("payload-2")
        val pending = outbox.getAllPending()
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.state == OutboxState.PENDING })
    }

    @Test
    fun inMemoryOutbox_markSubmitted_dropsFromPending() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id = outbox.enqueue("payload-1")
        outbox.markSubmitted(id)
        assertTrue(outbox.getAllPending().isEmpty())
    }
}
