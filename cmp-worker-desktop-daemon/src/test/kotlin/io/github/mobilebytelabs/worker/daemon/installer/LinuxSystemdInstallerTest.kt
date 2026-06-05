package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LinuxSystemdInstallerTest {

    @Test
    fun constructor_doesNotThrow() {
        val installer = LinuxSystemdInstaller()
        assertNotNull(installer)
    }

    @Test
    fun probe_returnsLinuxOsFamily() {
        val cap = LinuxSystemdInstaller().probe()
        assertEquals(OsFamily.LINUX, cap.osFamily)
    }

    @Test
    fun probe_doesNotThrow() {
        val cap = LinuxSystemdInstaller().probe()
        assertNotNull(cap)
        assertNotNull(cap.notes)
    }

    @Test
    fun probe_hasSchtasksFalse() {
        val cap = LinuxSystemdInstaller().probe()
        assertFalse(cap.hasSchtasks)
    }

    @Test
    fun probe_hasLaunchctlFalse() {
        val cap = LinuxSystemdInstaller().probe()
        assertFalse(cap.hasLaunchctl)
    }

    @Test
    fun isInstalled_unknownAppId_doesNotThrow() {
        LinuxSystemdInstaller().isInstalled("com.example.test-never-installed")
    }
}
