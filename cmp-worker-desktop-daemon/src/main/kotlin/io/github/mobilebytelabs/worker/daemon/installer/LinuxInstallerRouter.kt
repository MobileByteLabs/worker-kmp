package io.github.mobilebytelabs.worker.daemon.installer

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig
import io.github.mobilebytelabs.worker.daemon.DesktopBackgroundInstaller
import io.github.mobilebytelabs.worker.daemon.DesktopOsCapability
import io.github.mobilebytelabs.worker.daemon.InstallResult
import io.github.mobilebytelabs.worker.daemon.OsFamily

/**
 * Routes Linux installs to either [LinuxSystemdInstaller] (preferred) or [LinuxCronInstaller]
 * (fallback) based on tool availability at construction time.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 *
 * Preference order:
 * 1. `systemctl --user --version` exits 0 → `LinuxSystemdInstaller`
 * 2. `crontab -l` exits 0 or 1 (1 = no crontab yet but binary present) → `LinuxCronInstaller`
 * 3. Otherwise: every operation returns `InstallResult.Failure("No scheduler available")`.
 */
internal object LinuxInstallerRouter {

    private val log = Logger.withTag("worker-kmp-daemon")

    fun resolve(): DesktopBackgroundInstaller {
        if (hasSystemctlUser()) {
            log.i { "LinuxInstallerRouter: selecting LinuxSystemdInstaller" }
            return LinuxSystemdInstaller()
        }
        if (hasCron()) {
            log.i { "LinuxInstallerRouter: systemctl --user unavailable; selecting LinuxCronInstaller fallback" }
            return LinuxCronInstaller()
        }
        log.w { "LinuxInstallerRouter: neither systemctl --user nor crontab available" }
        return NoSchedulerInstaller
    }

    private fun hasSystemctlUser(): Boolean = try {
        ProcessBuilder("systemctl", "--user", "--version").redirectErrorStream(true).start().waitFor() == 0
    } catch (_: Exception) {
        false
    }

    private fun hasCron(): Boolean = try {
        // exit 0 = entries; exit 1 = no crontab but tool present; IOException = not installed
        ProcessBuilder("crontab", "-l").redirectErrorStream(true).start().let {
            it.waitFor()
            true
        }
    } catch (_: Exception) {
        false
    }

    private object NoSchedulerInstaller : DesktopBackgroundInstaller {
        override fun install(config: DesktopBackgroundConfig): InstallResult =
            InstallResult.Failure("No scheduler available on this Linux distro (need systemctl --user or crontab)")

        override fun uninstall(appId: String): InstallResult =
            InstallResult.Failure("No scheduler available on this Linux distro")

        override fun isInstalled(appId: String): Boolean = false

        override fun probe(): DesktopOsCapability = DesktopOsCapability(
            osFamily = OsFamily.LINUX,
            hasSchtasks = false,
            hasLaunchctl = false,
            hasSystemctlUser = false,
            hasCron = false,
            notes = "No scheduler available on this Linux distro",
        )
    }
}
