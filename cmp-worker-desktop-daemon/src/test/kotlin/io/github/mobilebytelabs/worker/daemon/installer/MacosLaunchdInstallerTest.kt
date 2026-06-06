package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MacosLaunchdInstallerTest {

    @Test
    fun constructor_doesNotThrow() {
        val installer = MacosLaunchdInstaller()
        assertNotNull(installer)
    }

    @Test
    fun probe_returnsMacosOsFamily() {
        val cap = MacosLaunchdInstaller().probe()
        assertEquals(OsFamily.MACOS, cap.osFamily)
    }

    @Test
    fun probe_doesNotThrow() {
        val cap = MacosLaunchdInstaller().probe()
        assertNotNull(cap)
        assertNotNull(cap.notes)
    }

    @Test
    fun probe_hasSchtasksFalse() {
        val cap = MacosLaunchdInstaller().probe()
        assertFalse(cap.hasSchtasks)
    }

    @Test
    fun probe_hasSystemctlUserFalse() {
        val cap = MacosLaunchdInstaller().probe()
        assertFalse(cap.hasSystemctlUser)
    }

    @Test
    fun probe_hasCronFalse() {
        val cap = MacosLaunchdInstaller().probe()
        assertFalse(cap.hasCron)
    }

    @Test
    fun isInstalled_unknownAppId_doesNotThrow() {
        MacosLaunchdInstaller().isInstalled("com.example.test-never-installed")
    }
}
