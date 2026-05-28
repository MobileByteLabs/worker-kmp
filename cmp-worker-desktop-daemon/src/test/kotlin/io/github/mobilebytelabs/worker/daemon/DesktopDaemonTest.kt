package io.github.mobilebytelabs.worker.daemon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

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

    /**
     * Verifies the OS family the factory selects matches `System.getProperty("os.name")`.
     * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X) — replaces the alpha05 assertion that
     * the stub returned OsFamily.OTHER.
     */
    @Test
    fun installer_probe_returnsCorrectOsForCurrentHost() {
        val installer = createDesktopBackgroundInstaller()
        val cap = installer.probe()
        val osName = System.getProperty("os.name", "").lowercase()
        val expected = when {
            osName.contains("win") -> OsFamily.WINDOWS
            osName.contains("mac") -> OsFamily.MACOS
            osName.contains("nux") || osName.contains("nix") || osName.contains("aix") -> OsFamily.LINUX
            else -> OsFamily.OTHER
        }
        assertEquals(expected, cap.osFamily, "probe should detect host OS family")
    }

    /**
     * LockFile must refuse a second acquire while the first is still held.
     * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
     */
    @Test
    fun lockFile_acquire_thenSecondAttemptFails(@TempDir tmp: Path) {
        val dir = tmp.toFile().absolutePath
        val first = LockFile(dir)
        assertTrue(first.tryAcquire(), "first acquire should succeed")
        try {
            val second = LockFile(dir)
            assertFalse(second.tryAcquire(), "second acquire should fail while first is held")
        } finally {
            first.release()
        }
        // After release, a fresh LockFile should be able to re-acquire.
        val third = LockFile(dir)
        assertTrue(third.tryAcquire(), "post-release acquire should succeed")
        third.release()
    }

    /**
     * JarIntegrityCheck.verify must return true (permissive) when no hash file exists —
     * common during dev/test runs where the daemon wasn't installed via the OS scheduler.
     * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
     */
    @Test
    fun jarIntegrityCheck_missingHashFile_isPermissive(@TempDir tmp: Path) {
        val dir = tmp.toFile().absolutePath
        // No daemon.jar.sha256 present.
        assertTrue(
            JarIntegrityCheck.verify(dir),
            "verify should return true when expected-hash file is absent",
        )
    }

    /**
     * JarIntegrityCheck.verify is also permissive when the hash file exists but is empty —
     * defends against partial writes during install.
     * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
     */
    @Test
    fun jarIntegrityCheck_emptyHashFile_isPermissive(@TempDir tmp: Path) {
        val dir = tmp.toFile().absolutePath
        File(dir, "daemon.jar.sha256").writeText("")
        assertTrue(
            JarIntegrityCheck.verify(dir),
            "verify should be permissive on empty hash file",
        )
    }
}
