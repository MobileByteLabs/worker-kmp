package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger as JulLogger

/**
 * File-based rotating logger for the daemon. Wraps [java.util.logging.FileHandler] with
 * a 1 MB-per-file × 3-file rotation policy.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Log file lives at `{persistenceDir}/logs/daemon.log` (rotated to `daemon.log.1`, `.2`).
 * Kermit's `withTag("worker-kmp-daemon")` continues to write to stdout — this side-channel
 * adds an auditable on-disk trail for `schtasks` / `launchd` / `systemd` invocations where
 * stdout is captured by the OS scheduler with non-trivial rotation policies.
 *
 * Usage:
 * ```kotlin
 * RotatingLogger.install(flags.persistenceDir)
 * Logger.withTag("worker-kmp-daemon").i { "Daemon starting" }   // → stdout + daemon.log
 * ```
 *
 * Best-effort: failures during install (e.g., persistenceDir not writable) are logged at
 * WARN to Kermit and otherwise silent — the daemon still runs.
 */
internal object RotatingLogger {

    private const val MAX_BYTES = 1_000_000
    private const val MAX_FILES = 3
    private var installed = false

    /**
     * Installs the rotating handler for the `worker-kmp-daemon` j.u.l. logger. Idempotent —
     * subsequent calls are no-ops.
     */
    @Synchronized
    fun install(persistenceDir: String) {
        if (installed) return
        val log = Logger.withTag("worker-kmp-daemon")
        try {
            val logDir = File("$persistenceDir/logs").apply { mkdirs() }
            val logFile = File(logDir, "daemon.log")
            // FileHandler pattern: %g = generation number (0..MAX_FILES-1), append=true
            val handler = FileHandler(
                logFile.absolutePath + ".%g",
                MAX_BYTES,
                MAX_FILES,
                /* append = */ true,
            )
            handler.formatter = SimpleLineFormatter
            handler.level = Level.ALL
            val jul = JulLogger.getLogger("worker-kmp-daemon")
            jul.useParentHandlers = false
            jul.addHandler(handler)
            jul.level = Level.ALL
            installed = true
            log.i { "RotatingLogger installed at ${logFile.absolutePath}.{0..${MAX_FILES - 1}} (max ${MAX_BYTES}B each)" }
        } catch (e: Exception) {
            log.w { "RotatingLogger.install failed: ${e.message} — file logging disabled, stdout only" }
        }
    }

    /** Forwards a log record to the file handler (in addition to whatever Kermit does). */
    fun log(level: Level, msg: String) {
        if (!installed) return
        runCatching { JulLogger.getLogger("worker-kmp-daemon").log(level, msg) }
    }

    private object SimpleLineFormatter : Formatter() {
        override fun format(record: LogRecord): String {
            val ts = java.time.Instant.ofEpochMilli(record.millis).toString()
            return "$ts ${record.level} ${record.message}\n"
        }
    }
}
