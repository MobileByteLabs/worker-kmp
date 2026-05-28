package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.daemon.installer.HashUtil
import java.io.File

/**
 * Defends against T1 (daemon-JAR tamper) per docs/operations/security.md.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Resolves the currently-executing JAR via its [ProtectionDomain.getCodeSource], computes
 * its SHA-256, compares against `{persistenceDir}/daemon.jar.sha256` (written by the
 * per-OS installer at install time). Mismatch returns false; daemon `main()` exits cleanly.
 *
 * Failure modes (all return true — fail-OPEN — so dev workflows aren't broken):
 * - Code source is null (running from classes, not a JAR — typical in dev/test).
 * - daemon.jar.sha256 absent (older install, or first run after an upgrade — installer
 *   re-writes the hash on next install/upgrade).
 *
 * Failure mode (returns false — fail-CLOSED):
 * - Both the JAR AND the recorded hash are present, but they disagree.
 */
internal object JarIntegrityCheck {

    private val log = Logger.withTag("worker-kmp-daemon")

    fun verify(persistenceDir: String): Boolean {
        val jarFile = currentJarFile() ?: run {
            log.i { "JarIntegrityCheck: not running from a JAR (dev/test); skipping" }
            return true
        }
        val hashFile = File("$persistenceDir/daemon.jar.sha256")
        if (!hashFile.exists()) {
            log.i { "JarIntegrityCheck: ${hashFile.absolutePath} missing — install hash not yet recorded; skipping" }
            return true
        }
        val expected = runCatching { hashFile.readText().trim() }.getOrNull()
        if (expected.isNullOrEmpty()) {
            log.w { "JarIntegrityCheck: ${hashFile.absolutePath} unreadable/empty; skipping" }
            return true
        }
        val actual = HashUtil.sha256Hex(jarFile)
        if (actual == null) {
            log.w { "JarIntegrityCheck: failed to hash $jarFile; skipping" }
            return true
        }
        if (actual.equals(expected, ignoreCase = true)) {
            log.i { "JarIntegrityCheck: OK (jar=$jarFile)" }
            return true
        }
        log.e {
            "JarIntegrityCheck: MISMATCH — jar=$jarFile expected=$expected actual=$actual. " +
                "Daemon refuses to run; investigate possible tamper or re-install."
        }
        return false
    }

    private fun currentJarFile(): File? = try {
        val location = JarIntegrityCheck::class.java.protectionDomain?.codeSource?.location ?: return null
        val file = File(location.toURI())
        if (file.exists() && file.isFile && file.name.endsWith(".jar", ignoreCase = true)) file else null
    } catch (e: Exception) {
        log.w { "JarIntegrityCheck: currentJarFile lookup failed: ${e.message}" }
        null
    }
}
