package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WindowsTaskInstallerTest {

    @Test
    fun constructor_doesNotThrow() {
        val installer = WindowsTaskInstaller()
        assertNotNull(installer)
    }

    @Test
    fun probe_returnsWindowsOsFamily() {
        val cap = WindowsTaskInstaller().probe()
        assertEquals(OsFamily.WINDOWS, cap.osFamily)
    }

    @Test
    fun probe_doesNotThrow() {
        val cap = WindowsTaskInstaller().probe()
        assertNotNull(cap)
        assertNotNull(cap.notes)
    }

    @Test
    fun probe_hasLaunchctlFalse() {
        val cap = WindowsTaskInstaller().probe()
        assertFalse(cap.hasLaunchctl)
    }

    @Test
    fun probe_hasSystemctlUserFalse() {
        val cap = WindowsTaskInstaller().probe()
        assertFalse(cap.hasSystemctlUser)
    }

    @Test
    fun probe_hasCronFalse() {
        val cap = WindowsTaskInstaller().probe()
        assertFalse(cap.hasCron)
    }

    @Test
    fun isInstalled_unknownAppId_doesNotThrow() {
        WindowsTaskInstaller().isInstalled("com.example.test-never-installed")
    }
}
