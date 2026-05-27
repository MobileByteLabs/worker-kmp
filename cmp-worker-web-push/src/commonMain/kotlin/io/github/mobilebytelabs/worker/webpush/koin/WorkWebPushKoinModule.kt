package io.github.mobilebytelabs.worker.webpush.koin

import io.github.mobilebytelabs.worker.webpush.WebPushSubscriber
import io.github.mobilebytelabs.worker.webpush.createWebPushSubscriber
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module — exposes [WebPushSubscriber] resolved via the platform actual
 * factory [createWebPushSubscriber]. Consumer adds alongside `workKoinModule(...)`:
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         workKoinModule(config = WorkerConfig(), workers = workerRegistry { /* ... */ }),
 *         workWebPushKoinModule,
 *         appModule,
 *     )
 * }
 * ```
 *
 * On JVM/iOS the [WebPushSubscriber] is a log-only stub (Web Push is browser-only).
 * On JS/WasmJs the real implementation lands in v3.0.0-alpha06.X follow-ups.
 *
 * Added in v3.0.0-alpha06 (Phase 9 of the v3.0.0 epic).
 */
public val workWebPushKoinModule: Module = module {
    single<WebPushSubscriber> { createWebPushSubscriber() }
}
