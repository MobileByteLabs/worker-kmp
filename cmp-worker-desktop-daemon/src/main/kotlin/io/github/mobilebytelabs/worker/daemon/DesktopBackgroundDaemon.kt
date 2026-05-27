package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger

/**
 * Desktop background daemon — invoked by the host OS scheduler (Windows Task Scheduler /
 * macOS launchd / Linux systemd-user timer / Linux cron) to process pending work when
 * the consumer app is not running.
 *
 * Added in v3.0.0-alpha05 (Phase 8 of the v3.0.0 epic).
 *
 * Current state (alpha05): SCAFFOLD ONLY. Reads CLI flags, logs, exits. Full
 * persistence-file loading + work execution land in v3.0.0-alpha05.X follow-ups.
 *
 * CLI flags:
 *   --persistence-dir <path>      default: ~/.worker-kmp
 *   --max-runtime-seconds <n>     default: 120
 *   --log-file <path>             default: <persistence-dir>/daemon.log
 *   --probe                       print capabilities + exit
 */
public fun main(args: Array<String>) {
    val flags = parseFlags(args)
    val logger = Logger.withTag("worker-kmp-daemon")

    logger.i { "Desktop daemon starting (alpha05 scaffold). persistence-dir=${flags.persistenceDir}" }

    if (flags.probe) {
        logger.i { "PROBE: would read ${flags.persistenceDir}/*.properties, run pending work, exit." }
        logger.i { "PROBE: full implementation lands in v3.0.0-alpha05.X follow-up." }
        return
    }

    // alpha05.X delivers:
    //   1. LockFile.tryLock(daemon.lock) — fail if another daemon instance running
    //   2. JarIntegrityCheck — SHA-256 hash check against daemon.jar.sha256
    //   3. Read PropertiesFileWorkPersistence from persistenceDir
    //   4. For each ENQUEUED entry whose nextRun <= now AND constraints satisfied:
    //      execute via DesktopWorkManager.runOne()
    //   5. Write updated state back
    //   6. Release lock, exit
    logger.i { "alpha05 scaffold: daemon would process pending work here. exiting cleanly." }
}

internal data class DaemonFlags(
    val persistenceDir: String,
    val maxRuntimeSeconds: Int,
    val logFile: String,
    val probe: Boolean,
)

internal fun parseFlags(args: Array<String>): DaemonFlags {
    var persistenceDir = "${System.getProperty("user.home")}/.worker-kmp"
    var maxRuntimeSeconds = 120
    var logFile = "$persistenceDir/daemon.log"
    var probe = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--persistence-dir" -> { persistenceDir = args[++i]; logFile = "$persistenceDir/daemon.log" }
            "--max-runtime-seconds" -> maxRuntimeSeconds = args[++i].toInt()
            "--log-file" -> logFile = args[++i]
            "--probe" -> probe = true
            else -> { /* ignore unknown — forward-compatible */ }
        }
        i++
    }
    return DaemonFlags(persistenceDir, maxRuntimeSeconds, logFile, probe)
}
