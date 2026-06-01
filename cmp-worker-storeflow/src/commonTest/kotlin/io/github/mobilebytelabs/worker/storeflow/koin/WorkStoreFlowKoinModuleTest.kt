package io.github.mobilebytelabs.worker.storeflow.koin

import io.github.mobilebytelabs.worker.storeflow.submit.SubmitOutbox
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Covers `workStoreFlowKoinModule` — verifies the module registers a singleton
 * `SubmitOutbox<Any>` and that Koin resolves it as a single instance.
 */
class WorkStoreFlowKoinModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun resolvesSubmitOutbox() {
        val koin = startKoin {
            modules(workStoreFlowKoinModule)
        }.koin
        val outbox = koin.get<SubmitOutbox<Any>>()
        assertNotNull(outbox)
    }

    @Test
    fun submitOutboxIsSingleton() {
        val koin = startKoin {
            modules(workStoreFlowKoinModule)
        }.koin
        val a = koin.get<SubmitOutbox<Any>>()
        val b = koin.get<SubmitOutbox<Any>>()
        assertSame(a, b)
    }
}
