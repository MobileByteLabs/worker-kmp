package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.daemon.DesktopBackgroundInstaller
import io.github.mobilebytelabs.worker.daemon.OsFamily
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LinuxInstallerRouterTest {

    @Test
    fun resolve_returnsNonNull() {
        val installer: DesktopBackgroundInstaller = LinuxInstallerRouter.resolve()
        assertNotNull(installer)
    }

    @Test
    fun resolve_probe_returnsLinuxOsFamily() {
        val cap = LinuxInstallerRouter.resolve().probe()
        assertEquals(OsFamily.LINUX, cap.osFamily)
    }

    @Test
    fun resolve_probe_doesNotThrow() {
        val installer = LinuxInstallerRouter.resolve()
        val cap = installer.probe()
        assertNotNull(cap)
        assertNotNull(cap.notes)
    }

    @Test
    fun resolve_isInstalled_doesNotThrow() {
        val installer = LinuxInstallerRouter.resolve()
        installer.isInstalled("com.example.test")
    }
}
