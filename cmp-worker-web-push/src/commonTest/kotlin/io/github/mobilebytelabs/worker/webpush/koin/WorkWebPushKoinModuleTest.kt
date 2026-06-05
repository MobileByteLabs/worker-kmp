package io.github.mobilebytelabs.worker.webpush.koin

import io.github.mobilebytelabs.worker.webpush.WebPushSubscriber
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertNotNull

class WorkWebPushKoinModuleTest {

    @Test
    fun workWebPushKoinModule_isNotNull() {
        assertNotNull(workWebPushKoinModule)
    }

    @Test
    fun workWebPushKoinModule_providesWebPushSubscriber() {
        val app = startKoin { modules(workWebPushKoinModule) }
        val sub = app.koin.get<WebPushSubscriber>()
        assertNotNull(sub)
        stopKoin()
    }
}
