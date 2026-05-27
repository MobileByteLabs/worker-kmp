package io.github.mobilebytelabs.worker.webpush

import co.touchlab.kermit.Logger

public actual fun createWebPushSubscriber(): WebPushSubscriber = StubWebPushSubscriber("JVM")

internal class StubWebPushSubscriber(private val platform: String) : WebPushSubscriber {
    override suspend fun ensureSubscribed(config: WebPushConfig): WebPushSubscription? {
        Logger.withTag("worker-kmp-web-push").i {
            "ensureSubscribed on $platform (stub — alpha06.X delivers real impl on JS/WasmJs). enabled=${config.enabled}"
        }
        return null
    }
    override suspend fun unsubscribe(): Boolean = false
    override suspend fun currentSubscription(): WebPushSubscription? = null
    override val pushSupported: Boolean = false
    override val requiresPwaInstall: Boolean = false
}
