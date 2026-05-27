package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DesktopDaemonTest {

    @Test
    fun parseFlags_defaults_areSane() {
        val flags = parseFlags(emptyArray())
        assertTrue(flags.persistenceDir.endsWith("/.worker-kmp"))
        assertEquals(120, flags.maxRuntimeSeconds)
        assertFalse(flags.probe)
    }

    @Test
    fun parseFlags_probe_setsFlag() {
        val flags = parseFlags(arrayOf("--probe"))
        assertTrue(flags.probe)
    }

    @Test
    fun installer_probe_returnsOsFamily() {
        val installer = createDesktopBackgroundInstaller()
        val cap = installer.probe()
        assertNotEquals(OsFamily.OTHER, cap.osFamily, "probe should detect host OS")
    }

    @Test
    fun installer_install_returnsFailureInAlpha05Scaffold() {
        val installer = createDesktopBackgroundInstaller()
        val result = installer.install(
            DesktopBackgroundConfig(
                appId = "com.example.test",
                daemonJarPath = "/nonexistent/daemon.jar",
            ),
        )
        assertTrue(result is InstallResult.Failure)
    }
}
