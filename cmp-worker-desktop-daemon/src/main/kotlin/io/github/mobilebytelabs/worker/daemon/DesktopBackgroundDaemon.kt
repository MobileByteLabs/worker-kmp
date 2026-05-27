package io.github.mobilebytelabs.worker.daemon

import co.touchlab.kermit.Logger
import java.io.File
import java.util.Properties

/**
 * Desktop background daemon — invoked by the host OS scheduler (Windows Task Scheduler /
 * macOS launchd / Linux systemd-user timer / Linux cron) to process pending work when
 * the consumer app is not running.
 *
 * Added in v3.0.0-alpha05 (Phase 8 of the v3.0.0 epic). v3.0.0-alpha05.X (Phase 8 alpha05.X)
 * replaces the alpha05 log-only stub with real-work execution: LockFile single-instance
 * guard, JarIntegrityCheck against a stored SHA-256 of the daemon JAR, then a pass over
 * the persistence directory dispatching any ENQUEUED entries.
 *
 * Note: the persistence schema written by `cmp-worker-desktop`'s
 * `PropertiesFileWorkPersistence` only retains `state` + `runAttemptCount` + `tags` — not the
 * full `WorkRequest` payload (workerClass FQCN, inputData, constraints, retryConfig). Until
 * a deeper schema rev lands, the daemon can scan + report + reset stuck-RUNNING entries to
 * ENQUEUED so the consumer app picks them up when it next launches, but it can NOT yet
 * fabricate a `WorkRequest` from a row alone — that's tracked for v3.0.0-alpha05.X.Y.
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

    if (flags.probe) {
        val capability = createDesktopBackgroundInstaller().probe()
        println("OS: ${capability.osFamily}")
        println(
            "schtasks: ${capability.hasSchtasks} | launchctl: ${capability.hasLaunchctl} | " +
                "systemctl-user: ${capability.hasSystemctlUser} | cron: ${capability.hasCron}",
        )
        println("Notes: ${capability.notes}")
        return
    }

    RotatingLogger.install(flags.persistenceDir)
    logger.i {
        "Desktop daemon starting. persistence-dir=${flags.persistenceDir} max-runtime=${flags.maxRuntimeSeconds}s"
    }

    val lockFile = LockFile(flags.persistenceDir)
    if (!lockFile.tryAcquire()) {
        logger.i { "Another daemon instance is holding ${flags.persistenceDir}/daemon.lock — exiting." }
        return
    }
    try {
        // T1 defence — refuse to run if the JAR has been tampered.
        if (!JarIntegrityCheck.verify(flags.persistenceDir)) {
            logger.e { "JAR integrity check FAILED. Daemon refusing to run." }
            return
        }
        runPendingWorkLoop(flags, logger)
    } finally {
        lockFile.release()
    }
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
            "--persistence-dir" -> {
                persistenceDir = args[++i]
                logFile = "$persistenceDir/daemon.log"
            }

            "--max-runtime-seconds" -> maxRuntimeSeconds = args[++i].toInt()

            "--log-file" -> logFile = args[++i]

            "--probe" -> probe = true

            else -> { /* ignore unknown — forward-compatible */ }
        }
        i++
    }
    return DaemonFlags(persistenceDir, maxRuntimeSeconds, logFile, probe)
}

/**
 * Scans the persistence directory for `.properties` files and, within the configured runtime
 * budget, performs the work the daemon can actually do at the current persistence schema:
 *
 * 1. Counts files by state (ENQUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLED / BLOCKED).
 * 2. For any file in `RUNNING` state — the consumer app must have died mid-execution — flips
 *    it back to `ENQUEUED` so the consumer app picks it up on next launch. (This mirrors
 *    `DesktopWorkStateStore.restoreFromPersistence`, ensuring even a never-relaunched app
 *    gets stuck-RUNNING healed by the daemon.)
 * 3. Skips ENQUEUED rows — without the full WorkRequest payload (workerClass + inputData)
 *    they can't be dispatched directly from the daemon JVM. v3.0.0-alpha05.X.Y will land a
 *    richer persistence schema that survives a daemon-only execution path.
 */
private fun runPendingWorkLoop(flags: DaemonFlags, logger: Logger) {
    val deadline = System.currentTimeMillis() + flags.maxRuntimeSeconds * 1000L
    val dir = File(flags.persistenceDir)
    if (!dir.exists() || !dir.isDirectory) {
        logger.i { "Persistence dir ${dir.absolutePath} missing — nothing to do." }
        return
    }
    val files = dir.listFiles { f -> f.extension == "properties" } ?: emptyArray()
    if (files.isEmpty()) {
        logger.i { "No .properties files in ${dir.absolutePath} — nothing to do." }
        return
    }

    var enqueued = 0
    var running = 0
    var succeeded = 0
    var failed = 0
    var cancelled = 0
    var blocked = 0
    var healed = 0

    for (file in files) {
        if (System.currentTimeMillis() > deadline) {
            logger.w {
                "Deadline exceeded mid-scan; exiting (processed=${enqueued + running + succeeded + failed + cancelled + blocked} of ${files.size})."
            }
            break
        }
        val props = Properties()
        val loadResult = runCatching {
            file.inputStream().use { props.load(it) }
        }
        if (loadResult.isFailure) {
            logger.w { "Skipping unreadable file=${file.name}: ${loadResult.exceptionOrNull()?.message}" }
            continue
        }
        when (props.getProperty("state")) {
            "ENQUEUED" -> {
                enqueued++
                val name = file.nameWithoutExtension
                logger.i {
                    "ENQUEUED work $name — daemon cannot dispatch without payload; " +
                        "consumer app picks it up on next launch (alpha05.X.Y will close the gap)."
                }
            }

            "RUNNING" -> {
                running++
                // The consumer app crashed mid-execution. Flip back to ENQUEUED.
                props.setProperty("state", "ENQUEUED")
                runCatching {
                    file.outputStream().use {
                        props.store(it, "Healed by worker-kmp daemon (was RUNNING after consumer-app death)")
                    }
                    healed++
                }.onFailure {
                    logger.w { "Failed to heal RUNNING→ENQUEUED for ${file.name}: ${it.message}" }
                }
            }

            "SUCCEEDED" -> succeeded++

            "FAILED" -> failed++

            "CANCELLED" -> cancelled++

            "BLOCKED" -> blocked++

            null -> logger.w { "Skipping file=${file.name}: no `state` property" }

            else -> logger.w { "Skipping file=${file.name}: unknown state=${props.getProperty("state")}" }
        }
    }
    logger.i {
        "Daemon scan complete: total=${files.size} enqueued=$enqueued running→healed=$healed " +
            "succeeded=$succeeded failed=$failed cancelled=$cancelled blocked=$blocked"
    }
}
