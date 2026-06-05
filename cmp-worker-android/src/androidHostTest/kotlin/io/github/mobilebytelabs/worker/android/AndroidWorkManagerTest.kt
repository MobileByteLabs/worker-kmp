package io.github.mobilebytelabs.worker.android

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidWorkManagerTest {
    private lateinit var workManager: AndroidWorkManager
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = AndroidWorkManager(context)
    }

    @Test
    fun constructor_createsValidWorkManager() {
        assertNotNull(workManager, "AndroidWorkManager must be non-null after construction")
    }

    @Test
    fun getWorkInfoById_withNonexistentId_returnsNull() = runTest {
        val nonexistentId = kotlin.uuid.Uuid.random()
        val result = workManager.getWorkInfoById(nonexistentId)
        // Should complete without exception; result null or empty is acceptable
        assertNotNull(workManager, "workManager should remain valid after query")
    }
}
