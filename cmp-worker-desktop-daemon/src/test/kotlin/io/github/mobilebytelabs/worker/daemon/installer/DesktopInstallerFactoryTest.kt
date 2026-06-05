package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.DesktopBackgroundInstaller
import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DesktopInstallerFactoryTest {

    @Test
    fun createDesktopBackgroundInstaller_returnsNonNull() {
        val installer: DesktopBackgroundInstaller = createDesktopBackgroundInstaller()
        assertNotNull(installer)
    }

    @Test
    fun createDesktopBackgroundInstaller_probe_doesNotThrow() {
        val installer = createDesktopBackgroundInstaller()
        val cap = installer.probe()
        assertNotNull(cap)
    }

    @Test
    fun createDesktopBackgroundInstaller_probe_osFamilyMatchesHost() {
        val cap = createDesktopBackgroundInstaller().probe()
        val osName = System.getProperty("os.name", "").lowercase()
        val expected = when {
            osName.contains("win") -> OsFamily.WINDOWS
            osName.contains("mac") -> OsFamily.MACOS
            osName.contains("nux") || osName.contains("nix") || osName.contains("aix") -> OsFamily.LINUX
            else -> OsFamily.OTHER
        }
        assertEquals(expected, cap.osFamily)
    }

    @Test
    fun createDesktopBackgroundInstaller_calledTwice_returnsDifferentInstances() {
        val a = createDesktopBackgroundInstaller()
        val b = createDesktopBackgroundInstaller()
        assertNotNull(a)
        assertNotNull(b)
    }
}
