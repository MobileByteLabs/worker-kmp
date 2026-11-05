@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker.android

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.mobilebytelabs.worker.ExperimentalForegroundApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidForegroundBridgeTest {
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun bridge_isAccessible() {
        // AndroidForegroundBridge is an object — verify it can be referenced
        assertNotNull(AndroidForegroundBridge, "AndroidForegroundBridge object must be accessible")
    }

    @Test
    fun notificationCustomizer_appliesIconChannelAndActions_withTheWorkerId() {
        val workerId = kotlin.uuid.Uuid.random()
        var seenId: kotlin.uuid.Uuid? = null
        AndroidForegroundBridge.notificationCustomizer = AndroidForegroundBridge.NotificationCustomizer { _, id, _, b ->
            seenId = id
            b.setSmallIcon(android.R.drawable.stat_sys_download)
                .setChannelId("app.downloads")
                .addAction(0, "Pause", null)
        }
        try {
            val n = AndroidForegroundBridge.buildNotification(
                context,
                workerId,
                io.github.mobilebytelabs.worker.ForegroundInfo(notificationId = 1, title = "t", message = "m"),
            )
            kotlin.test.assertEquals(workerId, seenId)
            kotlin.test.assertEquals(android.R.drawable.stat_sys_download, n.smallIcon.resId)
            kotlin.test.assertEquals("app.downloads", n.channelId)
            kotlin.test.assertEquals("Pause", n.actions.single().title)
        } finally {
            AndroidForegroundBridge.notificationCustomizer = null
        }
    }

    @Test
    fun noCustomizer_keepsTheDefaults() {
        val n = AndroidForegroundBridge.buildNotification(
            context,
            kotlin.uuid.Uuid.random(),
            io.github.mobilebytelabs.worker.ForegroundInfo(notificationId = 1, title = "t", message = "m"),
        )
        kotlin.test.assertEquals(AndroidForegroundBridge.DEFAULT_CHANNEL_ID, n.channelId)
        kotlin.test.assertEquals(android.R.drawable.stat_notify_sync, n.smallIcon.resId)
    }

    @Test
    fun defaultChannelId_isNonEmpty() {
        val channelId = AndroidForegroundBridge.DEFAULT_CHANNEL_ID
        assertNotNull(channelId)
        assert(channelId.isNotEmpty()) { "DEFAULT_CHANNEL_ID must be non-empty" }
    }
}
