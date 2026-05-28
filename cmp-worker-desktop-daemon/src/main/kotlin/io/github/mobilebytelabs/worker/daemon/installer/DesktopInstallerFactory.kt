package io.github.mobilebytelabs.worker.daemon.installer

import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig
import io.github.mobilebytelabs.worker.daemon.DesktopBackgroundInstaller
import io.github.mobilebytelabs.worker.daemon.DesktopOsCapability
import io.github.mobilebytelabs.worker.daemon.InstallResult
import io.github.mobilebytelabs.worker.daemon.OsFamily

/**
 * Same factory as [io.github.mobilebytelabs.worker.daemon.createDesktopBackgroundInstaller],
 * but under the [installer] package so the reflective lookup from `cmp-worker-desktop`
 * can target a stable fully-qualified name (`io.github.mobilebytelabs.worker.daemon.installer.DesktopInstallerFactoryKt`)
 * without coupling the desktop module to the daemon module's package-level functions.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X — Desktop daemon real impls).
 */
public fun createDesktopBackgroundInstaller(): DesktopBackgroundInstaller {
    val osName = System.getProperty("os.name", "").lowercase()
    return when {
        osName.contains("win") -> WindowsTaskInstaller()
        osName.contains("mac") -> MacosLaunchdInstaller()
        osName.contains("nux") || osName.contains("nix") || osName.contains("aix") -> LinuxInstallerRouter.resolve()
        else -> UnsupportedOsInstaller(osName)
    }
}

private class UnsupportedOsInstaller(private val osName: String) : DesktopBackgroundInstaller {
    override fun install(config: DesktopBackgroundConfig): InstallResult =
        InstallResult.Failure("Unsupported OS: $osName")

    override fun uninstall(appId: String): InstallResult = InstallResult.Failure("Unsupported OS: $osName")

    override fun isInstalled(appId: String): Boolean = false

    override fun probe(): DesktopOsCapability = DesktopOsCapability(
        osFamily = OsFamily.OTHER,
        hasSchtasks = false,
        hasLaunchctl = false,
        hasSystemctlUser = false,
        hasCron = false,
        notes = "Unsupported OS: $osName",
    )
}
