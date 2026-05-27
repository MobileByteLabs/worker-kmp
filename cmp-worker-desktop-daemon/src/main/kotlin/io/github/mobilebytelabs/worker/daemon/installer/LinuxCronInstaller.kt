package io.github.mobilebytelabs.worker.daemon.installer

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig
import io.github.mobilebytelabs.worker.daemon.DesktopBackgroundInstaller
import io.github.mobilebytelabs.worker.daemon.DesktopOsCapability
import io.github.mobilebytelabs.worker.daemon.InstallResult
import io.github.mobilebytelabs.worker.daemon.OsFamily
import io.github.mobilebytelabs.worker.daemon.resolvedPersistenceDir
import java.io.File

/**
 * Linux cron fallback installer.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Reads the current user's crontab via `crontab -l`, appends:
 * `*​/{pollIntervalMin} * * * * java -jar {daemonJarPath} --persistence-dir {persistenceDir} # worker-kmp:{appId}`
 *
 * On uninstall, strips the matching `# worker-kmp:{appId}` comment line. Pipes the modified
 * crontab back via `crontab -`.
 *
 * `isInstalled(appId)` greps the live crontab for the marker comment.
 */
internal class LinuxCronInstaller : DesktopBackgroundInstaller {

    private val log = Logger.withTag("worker-kmp-daemon")

    override fun install(config: DesktopBackgroundConfig): InstallResult {
        return try {
            val javaCmd = config.runtimeJavaHome ?: "/usr/bin/java"
            val marker = "# worker-kmp:${config.appId}"
            val persistenceDir = config.resolvedPersistenceDir()
            val newLine = "*/${config.pollIntervalMin} * * * * $javaCmd -jar ${config.daemonJarPath} " +
                "--persistence-dir $persistenceDir $marker"
            val existing = readCrontab().filterNot { it.endsWith(marker) }
            val updated = existing + newLine
            val result = writeCrontab(updated)
            if (result.exit == 0) {
                writeDaemonJarHash(config)
                log.i { "LinuxCronInstaller.install: cron line added for ${config.appId}" }
                InstallResult.Success
            } else {
                InstallResult.Failure("crontab write exit=${result.exit}; ${result.out}")
            }
        } catch (e: Exception) {
            InstallResult.Failure("LinuxCronInstaller.install threw: ${e.message}")
        }
    }

    override fun uninstall(appId: String): InstallResult {
        return try {
            val marker = "# worker-kmp:$appId"
            val existing = readCrontab().filterNot { it.endsWith(marker) }
            val result = writeCrontab(existing)
            if (result.exit == 0) {
                InstallResult.Success
            } else {
                InstallResult.Failure("crontab write exit=${result.exit}; ${result.out}")
            }
        } catch (e: Exception) {
            InstallResult.Failure("LinuxCronInstaller.uninstall threw: ${e.message}")
        }
    }

    override fun isInstalled(appId: String): Boolean {
        val marker = "# worker-kmp:$appId"
        return readCrontab().any { it.endsWith(marker) }
    }

    override fun probe(): DesktopOsCapability {
        val cron = try {
            ProcessBuilder("crontab", "-l").redirectErrorStream(true).start().let { p ->
                p.waitFor()
                // exit 0 = entries; exit 1 = no crontab yet but tool present. Both OK.
                true
            }
        } catch (_: Exception) {
            false
        }
        return DesktopOsCapability(
            osFamily = OsFamily.LINUX,
            hasSchtasks = false,
            hasLaunchctl = false,
            hasSystemctlUser = false,
            hasCron = cron,
            notes = if (cron) "crontab available (fallback path)" else "crontab NOT available",
        )
    }

    private fun readCrontab(): List<String> {
        return try {
            val proc = ProcessBuilder("crontab", "-l").redirectErrorStream(true).start()
            val text = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            // `crontab -l` exits 1 when no crontab exists; treat as empty.
            if (text.startsWith("no crontab", ignoreCase = true)) emptyList()
            else text.lines().filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class RunOut(val exit: Int, val out: String)

    private fun writeCrontab(lines: List<String>): RunOut = try {
        val tmp = File.createTempFile("worker-kmp-cron-", ".txt")
        try {
            tmp.writeText(lines.joinToString("\n", postfix = "\n"))
            val proc = ProcessBuilder("crontab", tmp.absolutePath).redirectErrorStream(true).start()
            val exit = proc.waitFor()
            val out = proc.inputStream.bufferedReader().readText().trim()
            RunOut(exit, out)
        } finally {
            tmp.delete()
        }
    } catch (e: Exception) {
        RunOut(-1, e.message.orEmpty())
    }

    private fun writeDaemonJarHash(config: DesktopBackgroundConfig) {
        val daemonJar = File(config.daemonJarPath)
        val hash = HashUtil.sha256Hex(daemonJar) ?: run {
            log.w { "writeDaemonJarHash: daemonJar=${daemonJar.absolutePath} unreadable; skipping" }
            return
        }
        runCatching {
            val outDir = File(config.resolvedPersistenceDir()).apply { mkdirs() }
            File(outDir, "daemon.jar.sha256").writeText(hash)
        }.onFailure {
            log.w { "writeDaemonJarHash failed: ${it.message}" }
        }
    }
}
