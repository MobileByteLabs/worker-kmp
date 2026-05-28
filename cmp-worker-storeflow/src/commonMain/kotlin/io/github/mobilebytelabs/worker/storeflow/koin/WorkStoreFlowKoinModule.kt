package io.github.mobilebytelabs.worker.storeflow.koin

import io.github.mobilebytelabs.worker.storeflow.submit.InMemorySubmitOutbox
import io.github.mobilebytelabs.worker.storeflow.submit.SubmitOutbox
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module — provides default in-memory implementations of storeflow types.
 * Per-platform persistent impls land per `cmp-worker-storeflow` v3.0.0-alpha03.X
 * (each platform overrides the in-memory binding).
 *
 * Consumer adds alongside workKoinModule(...) and workStore5KoinModule.
 *
 * Added in v3.0.0-alpha03.
 */
public val workStoreFlowKoinModule: Module = module {
    // Use a starProjection here so consumer code can resolve SubmitOutbox<MyPayload>
    // — note: Koin's parameterized-type resolution requires explicit qualifier or
    // separate consumer modules per payload type. v3.0.0-alpha03.X formalizes this.
    single<SubmitOutbox<Any>> { InMemorySubmitOutbox() }
}
