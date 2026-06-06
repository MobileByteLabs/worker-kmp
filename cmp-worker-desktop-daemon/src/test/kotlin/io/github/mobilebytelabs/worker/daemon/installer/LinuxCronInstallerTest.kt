package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LinuxCronInstallerTest {

    @Test
    fun constructor_doesNotThrow() {
        val installer = LinuxCronInstaller()
        assertNotNull(installer)
    }

    @Test
    fun probe_returnsLinuxOsFamily() {
        val cap = LinuxCronInstaller().probe()
        assertEquals(OsFamily.LINUX, cap.osFamily)
    }

    @Test
    fun probe_doesNotThrow() {
        val cap = LinuxCronInstaller().probe()
        assertNotNull(cap)
        assertNotNull(cap.notes)
    }

    @Test
    fun probe_hasSchtasksFalse() {
        val cap = LinuxCronInstaller().probe()
        assertFalse(cap.hasSchtasks)
    }

    @Test
    fun probe_hasLaunchctlFalse() {
        val cap = LinuxCronInstaller().probe()
        assertFalse(cap.hasLaunchctl)
    }

    @Test
    fun probe_hasSystemctlUserFalse() {
        val cap = LinuxCronInstaller().probe()
        assertFalse(cap.hasSystemctlUser)
    }

    @Test
    fun isInstalled_unknownAppId_doesNotThrow() {
        LinuxCronInstaller().isInstalled("com.example.test-never-installed")
    }
}
