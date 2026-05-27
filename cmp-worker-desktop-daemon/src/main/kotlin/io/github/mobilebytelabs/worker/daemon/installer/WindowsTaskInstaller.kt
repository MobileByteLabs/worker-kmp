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
 * Windows Task Scheduler installer using `schtasks /Create /XML`.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * The generated task:
 * - Triggers every [DesktopBackgroundConfig.pollIntervalMin] minutes.
 * - Runs at least-privilege ([RunLevel]=LeastPrivilege) with [LogonType]=InteractiveToken.
 * - Honors [DesktopBackgroundConfig.runOnlyIfLoggedOn].
 * - Caps each invocation at 10 minutes (`ExecutionTimeLimit`).
 * - Uses [DesktopBackgroundConfig.runtimeJavaHome] (or `javaw.exe` from PATH) to run the daemon JAR.
 *
 * On successful install, writes the daemon JAR's SHA-256 hex digest to
 * `{persistenceDir}/daemon.jar.sha256` — consumed by [io.github.mobilebytelabs.worker.daemon.JarIntegrityCheck]
 * at daemon start time.
 */
internal class WindowsTaskInstaller : DesktopBackgroundInstaller {

    private val log = Logger.withTag("worker-kmp-daemon")

    override fun install(config: DesktopBackgroundConfig): InstallResult {
        val taskName = "WorkerKmp-${config.appId}"
        val xmlFile = createTaskXml(config)
        return try {
            val proc = ProcessBuilder(
                "schtasks",
                "/Create",
                "/XML",
                xmlFile.absolutePath,
                "/TN",
                taskName,
                "/F", // force overwrite if exists
            ).redirectErrorStream(true).start()
            val exit = proc.waitFor()
            val output = proc.inputStream.bufferedReader().readText().trim()
            if (exit == 0) {
                writeDaemonJarHash(config)
                log.i { "WindowsTaskInstaller.install: task=$taskName installed" }
                InstallResult.Success
            } else {
                InstallResult.Failure("schtasks /Create exit=$exit; output=$output")
            }
        } catch (e: Exception) {
            InstallResult.Failure("schtasks /Create threw: ${e.message}")
        } finally {
            runCatching { xmlFile.delete() }
        }
    }

    override fun uninstall(appId: String): InstallResult {
        val taskName = "WorkerKmp-$appId"
        return try {
            val proc = ProcessBuilder("schtasks", "/Delete", "/TN", taskName, "/F")
                .redirectErrorStream(true).start()
            val exit = proc.waitFor()
            if (exit == 0) InstallResult.Success else InstallResult.Failure("schtasks /Delete exit=$exit")
        } catch (e: Exception) {
            InstallResult.Failure("schtasks /Delete threw: ${e.message}")
        }
    }

    override fun isInstalled(appId: String): Boolean {
        val taskName = "WorkerKmp-$appId"
        return try {
            val proc = ProcessBuilder("schtasks", "/Query", "/TN", taskName)
                .redirectErrorStream(true).start()
            proc.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    override fun probe(): DesktopOsCapability {
        val schtasks = try {
            ProcessBuilder("schtasks", "/?").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
        return DesktopOsCapability(
            osFamily = OsFamily.WINDOWS,
            hasSchtasks = schtasks,
            hasLaunchctl = false,
            hasSystemctlUser = false,
            hasCron = false,
            notes = if (schtasks) "schtasks available" else "schtasks NOT available — daemon install will fail",
        )
    }

    private fun createTaskXml(config: DesktopBackgroundConfig): File {
        val javaCmd = config.runtimeJavaHome ?: "javaw.exe"
        val persistenceDir = config.resolvedPersistenceDir()
        val xml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n")
            append("<Task version=\"1.4\" xmlns=\"http://schemas.microsoft.com/windows/2004/02/mit/task\">\n")
            append("  <RegistrationInfo>\n")
            append("    <Description>worker-kmp daemon for ${config.appId}</Description>\n")
            append("  </RegistrationInfo>\n")
            append("  <Triggers>\n")
            append("    <TimeTrigger>\n")
            append("      <Repetition>\n")
            append("        <Interval>PT${config.pollIntervalMin}M</Interval>\n")
            append("      </Repetition>\n")
            append("      <StartBoundary>2026-01-01T00:00:00</StartBoundary>\n")
            append("      <Enabled>true</Enabled>\n")
            append("    </TimeTrigger>\n")
            append("  </Triggers>\n")
            append("  <Principals>\n")
            append("    <Principal id=\"Author\">\n")
            append("      <LogonType>InteractiveToken</LogonType>\n")
            append("      <RunLevel>LeastPrivilege</RunLevel>\n")
            append("    </Principal>\n")
            append("  </Principals>\n")
            append("  <Settings>\n")
            append("    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>\n")
            append("    <RunOnlyIfLoggedOn>${config.runOnlyIfLoggedOn}</RunOnlyIfLoggedOn>\n")
            append("    <ExecutionTimeLimit>PT10M</ExecutionTimeLimit>\n")
            append("  </Settings>\n")
            append("  <Actions Context=\"Author\">\n")
            append("    <Exec>\n")
            append("      <Command>$javaCmd</Command>\n")
            append(
                "      <Arguments>-jar \"${config.daemonJarPath}\" --persistence-dir \"$persistenceDir\"</Arguments>\n",
            )
            append("    </Exec>\n")
            append("  </Actions>\n")
            append("</Task>\n")
        }
        val tmp = File.createTempFile("worker-kmp-task-", ".xml")
        tmp.writeText(xml, Charsets.UTF_16)
        return tmp
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
