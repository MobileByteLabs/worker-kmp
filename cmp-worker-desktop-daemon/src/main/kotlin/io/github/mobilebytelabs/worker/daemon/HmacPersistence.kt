package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 envelope around the persistence directory's `.properties` files.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Defends T2 (persistence-file tamper) per docs/operations/security.md by:
 * 1. Generating a per-install ephemeral 32-byte key at `{persistenceDir}/.persistence-hmac.key`
 *    (POSIX 600 permissions on POSIX file systems).
 * 2. On write, computing HMAC-SHA256 over the file content and stamping it into the
 *    `.properties` file as a `#__hmac=<hex>` comment line at the top.
 * 3. On read, stripping the HMAC line, recomputing, and rejecting (returning null + logging)
 *    on mismatch.
 *
 * The HMAC comment line is a comment in the `java.util.Properties` format so existing readers
 * that don't know about HMAC simply ignore it.
 */
internal class HmacPersistence(persistenceDir: String) {

    private val log = Logger.withTag("worker-kmp-daemon")
    private val dir = File(persistenceDir).apply { mkdirs() }
    private val keyFile = File(dir, ".persistence-hmac.key")
    private val key: ByteArray by lazy { loadOrCreateKey() }

    /** Stamp the given properties-file content with an HMAC and write to [file]. */
    fun writeStamped(file: File, contentLines: List<String>) {
        val body = contentLines.joinToString("\n", postfix = "\n")
        val hmac = computeHmac(body.toByteArray(Charsets.UTF_8))
        val stamped = "$HMAC_PREFIX$hmac\n$body"
        file.writeText(stamped, Charsets.UTF_8)
    }

    /**
     * Read [file], verify the HMAC, and return the content lines (excluding the HMAC stamp)
     * on success. Returns null on missing-HMAC or HMAC-mismatch (logged WARN).
     */
    fun readVerified(file: File): List<String>? {
        if (!file.exists()) return null
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        val lines = raw.lines()
        if (lines.isEmpty() || !lines[0].startsWith(HMAC_PREFIX)) {
            log.w { "HmacPersistence.readVerified: file=${file.name} missing HMAC stamp; rejecting" }
            return null
        }
        val expected = lines[0].removePrefix(HMAC_PREFIX).trim()
        // body = everything after the HMAC line; preserve trailing newline shape used in writeStamped.
        val body = lines.drop(1).joinToString("\n")
        val actual = computeHmac(body.toByteArray(Charsets.UTF_8))
        if (!constantTimeEquals(expected, actual)) {
            log.w { "HmacPersistence.readVerified: file=${file.name} HMAC mismatch; rejecting" }
            return null
        }
        // Strip trailing-newline noise the writer added.
        return lines.drop(1).dropLastWhile { it.isEmpty() }
    }

    private fun computeHmac(data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    private fun loadOrCreateKey(): ByteArray {
        if (keyFile.exists()) {
            val bytes = runCatching { keyFile.readBytes() }.getOrNull()
            if (bytes != null && bytes.size == KEY_LEN) return bytes
            log.w { "HmacPersistence: ${keyFile.name} corrupt (size=${bytes?.size}); regenerating" }
        }
        val random = ByteArray(KEY_LEN).also { SecureRandom().nextBytes(it) }
        keyFile.parentFile?.mkdirs()
        keyFile.writeBytes(random)
        // Best-effort POSIX 600 on POSIX file systems.
        runCatching {
            Files.setPosixFilePermissions(keyFile.toPath(), PosixFilePermissions.fromString("rw-------"))
        }
        return random
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(a.toByteArray()).contentEquals(md.digest(b.toByteArray()))
    }

    internal companion object {
        const val HMAC_PREFIX = "#__hmac="
        private const val KEY_LEN = 32
    }
}
