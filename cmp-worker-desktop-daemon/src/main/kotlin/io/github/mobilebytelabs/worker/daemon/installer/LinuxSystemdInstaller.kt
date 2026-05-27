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
 * Linux systemd --user installer.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Writes 2 unit files to `~/.config/systemd/user/`:
 * - `{appId}.worker-kmp.service` (Type=oneshot ExecStart=java -jar ...)
 * - `{appId}.worker-kmp.timer` (OnBootSec=2min OnUnitActiveSec={pollIntervalMin}min Persistent=true)
 *
 * Then runs `systemctl --user daemon-reload && systemctl --user enable --now {appId}.worker-kmp.timer`.
 *
 * Detects user lingering via `loginctl show-user $USER --property=Linger`; if `no`,
 * logs a WARNING (timer pauses on logout — consumer needs `loginctl enable-linger $USER`).
 */
internal class LinuxSystemdInstaller : DesktopBackgroundInstaller {

    private val log = Logger.withTag("worker-kmp-daemon")

    override fun install(config: DesktopBackgroundConfig): InstallResult {
        return try {
            val unitDir = unitDir().apply { mkdirs() }
            val serviceFile = File(unitDir, "${config.appId}.worker-kmp.service")
            val timerFile = File(unitDir, "${config.appId}.worker-kmp.timer")
            serviceFile.writeText(buildServiceUnit(config))
            timerFile.writeText(buildTimerUnit(config))
            // Reload, enable+start.
            val reload = run("systemctl", "--user", "daemon-reload")
            if (reload.exit != 0) return InstallResult.Failure("daemon-reload exit=${reload.exit}; ${reload.out}")
            val enable = run("systemctl", "--user", "enable", "--now", "${config.appId}.worker-kmp.timer")
            if (enable.exit != 0) return InstallResult.Failure("systemctl enable exit=${enable.exit}; ${enable.out}")
            checkLinger()
            writeDaemonJarHash(config)
            log.i { "LinuxSystemdInstaller.install: timer ${config.appId}.worker-kmp.timer enabled" }
            InstallResult.Success
        } catch (e: Exception) {
            InstallResult.Failure("LinuxSystemdInstaller.install threw: ${e.message}")
        }
    }

    override fun uninstall(appId: String): InstallResult = try {
        val unitDir = unitDir()
        val serviceFile = File(unitDir, "$appId.worker-kmp.service")
        val timerFile = File(unitDir, "$appId.worker-kmp.timer")
        // Best-effort stop+disable before deleting; tolerate non-zero exit (e.g., already stopped).
        run("systemctl", "--user", "disable", "--now", "$appId.worker-kmp.timer")
        serviceFile.delete()
        timerFile.delete()
        run("systemctl", "--user", "daemon-reload")
        InstallResult.Success
    } catch (e: Exception) {
        InstallResult.Failure("LinuxSystemdInstaller.uninstall threw: ${e.message}")
    }

    override fun isInstalled(appId: String): Boolean {
        val timerFile = File(unitDir(), "$appId.worker-kmp.timer")
        return timerFile.exists()
    }

    override fun probe(): DesktopOsCapability {
        val systemctlUser = try {
            ProcessBuilder("systemctl", "--user", "--version").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
        val cron = try {
            // `crontab -l` exits 0 (entries exist) or 1 (no crontab yet) when crontab is installed;
            // non-zero from "command not found" surfaces via IOException above.
            ProcessBuilder("crontab", "-l").redirectErrorStream(true).start().let {
                it.waitFor()
                true
            }
        } catch (_: Exception) {
            false
        }
        return DesktopOsCapability(
            osFamily = OsFamily.LINUX,
            hasSchtasks = false,
            hasLaunchctl = false,
            hasSystemctlUser = systemctlUser,
            hasCron = cron,
            notes = when {
                systemctlUser -> "systemctl --user available"
                cron -> "systemctl --user unavailable; cron fallback available"
                else -> "Neither systemctl --user nor crontab available"
            },
        )
    }

    private fun unitDir(): File = File("${System.getProperty("user.home")}/.config/systemd/user")

    private fun buildServiceUnit(config: DesktopBackgroundConfig): String {
        val javaCmd = config.runtimeJavaHome ?: "/usr/bin/java"
        val persistenceDir = config.resolvedPersistenceDir()
        return """
            |[Unit]
            |Description=worker-kmp service for ${config.appId}
            |
            |[Service]
            |Type=oneshot
            |ExecStart=$javaCmd -jar ${config.daemonJarPath} --persistence-dir $persistenceDir
            |
        """.trimMargin()
    }

    private fun buildTimerUnit(config: DesktopBackgroundConfig): String = """
            |[Unit]
            |Description=worker-kmp timer for ${config.appId}
            |
            |[Timer]
            |OnBootSec=2min
            |OnUnitActiveSec=${config.pollIntervalMin}min
            |Persistent=true
            |
            |[Install]
            |WantedBy=timers.target
            |
    """.trimMargin()

    private fun checkLinger() {
        try {
            val user = System.getProperty("user.name") ?: return
            val proc = ProcessBuilder("loginctl", "show-user", user, "--property=Linger")
                .redirectErrorStream(true).start()
            proc.waitFor()
            val output = proc.inputStream.bufferedReader().readText().trim()
            if (output.contains("Linger=no", ignoreCase = true)) {
                log.w {
                    "Linger is disabled for user=$user — the worker-kmp timer will pause on logout. " +
                        "Run `loginctl enable-linger $user` (or `sudo loginctl enable-linger $user`) to keep timers active."
                }
            }
        } catch (_: Exception) {
            // loginctl missing on minimal distros — skip silently.
        }
    }

    private data class RunOut(val exit: Int, val out: String)

    private fun run(vararg cmd: String): RunOut = try {
        val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val exit = proc.waitFor()
        val out = proc.inputStream.bufferedReader().readText().trim()
        RunOut(exit, out)
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
