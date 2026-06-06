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
    fun defaultChannelId_isNonEmpty() {
        val channelId = AndroidForegroundBridge.DEFAULT_CHANNEL_ID
        assertNotNull(channelId)
        assert(channelId.isNotEmpty()) { "DEFAULT_CHANNEL_ID must be non-empty" }
    }
}
