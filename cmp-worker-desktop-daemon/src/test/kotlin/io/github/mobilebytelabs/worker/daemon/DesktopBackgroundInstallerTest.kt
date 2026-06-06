package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DesktopBackgroundInstallerTest {

    @Test
    fun createDesktopBackgroundInstaller_returnsNonNull() {
        val installer = createDesktopBackgroundInstaller()
        assertNotNull(installer)
    }

    @Test
    fun probe_returnsValidOsCapability() {
        val cap = createDesktopBackgroundInstaller().probe()
        assertNotNull(cap)
        assertNotNull(cap.osFamily)
        assertNotNull(cap.notes)
    }

    @Test
    fun probe_osFamilyMatchesCurrentHost() {
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
    fun installResult_success_isDistinctType() {
        val result: InstallResult = InstallResult.Success
        assertTrue(result is InstallResult.Success)
    }

    @Test
    fun installResult_failure_holdsReason() {
        val result = InstallResult.Failure("access denied")
        assertEquals("access denied", result.reason)
        assertTrue(result is InstallResult.Failure)
    }

    @Test
    fun desktopOsCapability_dataClassEquality() {
        val cap1 = DesktopOsCapability(
            osFamily = OsFamily.LINUX,
            hasSchtasks = false,
            hasLaunchctl = false,
            hasSystemctlUser = true,
            hasCron = true,
            notes = "systemctl --user available",
        )
        val cap2 = cap1.copy()
        assertEquals(cap1, cap2)
    }

    @Test
    fun osFamilyEnum_allValuesPresent() {
        val values = OsFamily.values()
        assertTrue(values.any { it == OsFamily.WINDOWS })
        assertTrue(values.any { it == OsFamily.MACOS })
        assertTrue(values.any { it == OsFamily.LINUX })
        assertTrue(values.any { it == OsFamily.OTHER })
    }

    @Test
    fun desktopOsCapability_flagsAreBoolean() {
        val cap = createDesktopBackgroundInstaller().probe()
        // Verify flags are accessible; at least one boolean expression must be evaluable
        val anyFlag = cap.hasSchtasks || cap.hasLaunchctl || cap.hasSystemctlUser || cap.hasCron
        assertFalse(anyFlag && !anyFlag, "boolean flag access should not throw")
    }
}
