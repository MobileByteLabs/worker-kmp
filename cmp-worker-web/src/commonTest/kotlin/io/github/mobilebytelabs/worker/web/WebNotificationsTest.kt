package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebNotificationsTest {

    @Test
    fun notificationPermission_enumHasExpectedValues() {
        val values = NotificationPermission.values()
        assertTrue(values.any { it == NotificationPermission.GRANTED })
        assertTrue(values.any { it == NotificationPermission.DENIED })
        assertTrue(values.any { it == NotificationPermission.DEFAULT })
    }

    @Test
    fun requestNotificationPermission_returnsPermission() = runTest {
        val permission = requestNotificationPermission()
        assertNotNull(permission)
        assertTrue(
            permission == NotificationPermission.GRANTED ||
                permission == NotificationPermission.DENIED ||
                permission == NotificationPermission.DEFAULT,
        )
    }

    @Test
    fun showWorkerNotification_doesNotThrow() {
        showWorkerNotification(id = "test-id", title = "Test", body = "Test body")
    }

    @Test
    fun showWorkerNotification_withProgress_doesNotThrow() {
        showWorkerNotification(id = "test-id", title = "Syncing", body = "50% complete", progress = 50)
    }

    @Test
    fun showWorkerNotification_withHundredProgress_doesNotThrow() {
        showWorkerNotification(id = "test-id", title = "Done", body = "Sync complete", progress = 100)
    }
}
