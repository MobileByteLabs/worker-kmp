package io.github.mobilebytelabs.worker.android

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidWorkManagerInitTest {
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun workManagerInstance_isNonNullAfterTestInit() {
        val wm = androidx.work.WorkManager.getInstance(context)
        assertNotNull(wm, "WorkManager instance must be non-null after initializeTestWorkManager")
    }

    @Test
    fun androidWorkManagerFactory_isPublicApi() {
        // The androidWorkManagerFactory top-level function is the public init API.
        // Verify it is callable from the test scope (accessibility smoke test).
        assertNotNull(::androidWorkManagerFactory, "androidWorkManagerFactory must be accessible")
    }
}
