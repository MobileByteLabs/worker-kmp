package org.mifos.sync

import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister
import org.mifos.core.data.currency.CurrencyRepository
import org.mifos.core.data.economic.MacroIndicatorsRepository

/**
 * Sample consumer's DataSyncWorker — extends the library's AbstractDataSyncWorker
 * + lists this sample's specific Syncable repos (Currency + Macro).
 *
 * Library does the heavy lifting (fan-out, awaitAll, persistence); sample
 * declares the adopters.
 */
class DataSyncWorker(
    ctx: WorkerContext,
    currencyRepository: CurrencyRepository,
    macroIndicatorsRepository: MacroIndicatorsRepository,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(
    ctx,
    syncables = listOf(currencyRepository, macroIndicatorsRepository),
    persister = persister,
)
