package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

/**
 * OS-level file lock that prevents two daemon instances from running concurrently.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Uses `FileChannel.tryLock()` (advisory on Linux/macOS, mandatory on Windows). Writes
 * the current PID into the lock file for diagnostic purposes (the lock itself, not the
 * PID, is what enforces single-instance).
 */
internal class LockFile(persistenceDir: String) {

    private val pidFile = File("$persistenceDir/daemon.lock")
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /**
     * Attempts to acquire the lock. Returns true on success (caller MUST call [release]),
     * false if another daemon instance holds the lock.
     */
    fun tryAcquire(): Boolean = try {
        pidFile.parentFile?.mkdirs()
        val raf = RandomAccessFile(pidFile, "rw")
        channel = raf.channel
        lock = try {
            channel?.tryLock()
        } catch (_: Exception) {
            null
        }
        if (lock != null) {
            // Write our PID for diagnostics.
            runCatching {
                val pid = ProcessHandle.current().pid().toString()
                raf.setLength(0)
                raf.writeBytes(pid)
            }
            true
        } else {
            // Failed to acquire — close the channel we opened so we don't leak.
            runCatching { channel?.close() }
            channel = null
            false
        }
    } catch (e: Exception) {
        Logger.withTag("worker-kmp-daemon").w { "LockFile.tryAcquire failed: ${e.message}" }
        runCatching { channel?.close() }
        channel = null
        false
    }

    /** Releases the lock + deletes the pid file. Idempotent. */
    fun release() {
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        runCatching { pidFile.delete() }
        lock = null
        channel = null
    }
}
