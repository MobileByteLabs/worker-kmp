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
 * macOS launchd installer using `launchctl load -w`.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * The plist is written to `~/Library/LaunchAgents/{appId}.worker-kmp.plist`:
 * - `StartInterval`: pollIntervalMin × 60 seconds.
 * - `RunAtLoad`: true — runs once at login.
 * - `KeepAlive`: false — fires per-interval, not continuously.
 * - `ProcessType`: Background — yields scheduling time to interactive processes.
 *
 * Uninstall runs `launchctl unload -w` THEN deletes the plist; isInstalled checks the plist.
 */
internal class MacosLaunchdInstaller : DesktopBackgroundInstaller {

    private val log = Logger.withTag("worker-kmp-daemon")

    override fun install(config: DesktopBackgroundConfig): InstallResult {
        val plistFile = plistFile(config.appId)
        return try {
            plistFile.parentFile?.mkdirs()
            plistFile.writeText(buildPlist(config), Charsets.UTF_8)
            val proc = ProcessBuilder("launchctl", "load", "-w", plistFile.absolutePath)
                .redirectErrorStream(true).start()
            val exit = proc.waitFor()
            val output = proc.inputStream.bufferedReader().readText().trim()
            if (exit == 0) {
                writeDaemonJarHash(config)
                log.i { "MacosLaunchdInstaller.install: plist=${plistFile.absolutePath} loaded" }
                InstallResult.Success
            } else {
                InstallResult.Failure("launchctl load exit=$exit; output=$output")
            }
        } catch (e: Exception) {
            InstallResult.Failure("launchctl load threw: ${e.message}")
        }
    }

    override fun uninstall(appId: String): InstallResult {
        val plistFile = plistFile(appId)
        return try {
            val proc = ProcessBuilder("launchctl", "unload", "-w", plistFile.absolutePath)
                .redirectErrorStream(true).start()
            val exit = proc.waitFor()
            plistFile.delete()
            if (exit == 0) {
                InstallResult.Success
            } else {
                // Even if unload reports non-zero (e.g., not loaded), plist is gone — treat as success.
                log.w { "launchctl unload exit=$exit; plist deleted regardless" }
                InstallResult.Success
            }
        } catch (e: Exception) {
            InstallResult.Failure("launchctl unload threw: ${e.message}")
        }
    }

    override fun isInstalled(appId: String): Boolean = plistFile(appId).exists()

    override fun probe(): DesktopOsCapability {
        val launchctl = try {
            ProcessBuilder("launchctl", "version").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
        return DesktopOsCapability(
            osFamily = OsFamily.MACOS,
            hasSchtasks = false,
            hasLaunchctl = launchctl,
            hasSystemctlUser = false,
            hasCron = false,
            notes = if (launchctl) "launchctl available" else "launchctl NOT available — daemon install will fail",
        )
    }

    private fun plistFile(appId: String): File {
        val home = System.getProperty("user.home")
        return File("$home/Library/LaunchAgents/$appId.worker-kmp.plist")
    }

    private fun buildPlist(config: DesktopBackgroundConfig): String {
        val javaCmd = config.runtimeJavaHome ?: "/usr/bin/java"
        val interval = config.pollIntervalMin * 60
        val persistenceDir = config.resolvedPersistenceDir()
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append(
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n",
            )
            append("<plist version=\"1.0\">\n")
            append("<dict>\n")
            append("    <key>Label</key><string>${config.appId}.worker-kmp</string>\n")
            append("    <key>ProgramArguments</key>\n")
            append("    <array>\n")
            append("        <string>$javaCmd</string>\n")
            append("        <string>-jar</string>\n")
            append("        <string>${config.daemonJarPath}</string>\n")
            append("        <string>--persistence-dir</string>\n")
            append("        <string>$persistenceDir</string>\n")
            append("    </array>\n")
            append("    <key>StartInterval</key><integer>$interval</integer>\n")
            append("    <key>RunAtLoad</key><true/>\n")
            append("    <key>KeepAlive</key><false/>\n")
            append("    <key>ProcessType</key><string>Background</string>\n")
            append("</dict>\n")
            append("</plist>\n")
        }
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
