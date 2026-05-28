package io.github.mobilebytelabs.worker.daemon.installer

import java.io.File
import java.security.MessageDigest

/**
 * Internal SHA-256 helpers shared by [DesktopBackgroundInstaller] impls + [io.github.mobilebytelabs.worker.daemon.JarIntegrityCheck].
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 */
internal object HashUtil {

    /** SHA-256 hex digest of [file]'s bytes. Returns null when file is missing/unreadable. */
    fun sha256Hex(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = stream.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
