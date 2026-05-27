package io.github.mobilebytelabs.worker.daemon

/**
 * Configuration for installing the daemon via the host OS scheduler.
 *
 * Added in v3.0.0-alpha05.
 */
public data class DesktopBackgroundConfig(
    public val appId: String,                                  // reverse-DNS-ish
    public val daemonJarPath: String,                          // absolute path to daemon JAR
    public val runtimeJavaHome: String? = null,                // null → "java" from PATH
    public val persistenceDir: String = "${System.getProperty("user.home")}/.worker-kmp",
    public val pollIntervalMin: Int = 15,
    public val installOnFirstRun: Boolean = true,
    public val uninstallOnAppUninstall: Boolean = true,
    public val runOnlyIfLoggedOn: Boolean = false,
)

/**
 * Per-OS installer for the desktop daemon. Per-OS implementations:
 * - Windows: schtasks-based
 * - macOS: launchd user agent (~/Library/LaunchAgents/{appId}.plist)
 * - Linux: systemd --user (~/.config/systemd/user/{appId}.timer + .service); cron fallback
 *
 * Per-OS impls land in v3.0.0-alpha05.X follow-up. alpha05 ships the interface + factory.
 */
public interface DesktopBackgroundInstaller {
    public fun install(config: DesktopBackgroundConfig): InstallResult
    public fun uninstall(appId: String): InstallResult
    public fun isInstalled(appId: String): Boolean
    public fun probe(): DesktopOsCapability
}

public sealed class InstallResult {
    public object Success : InstallResult()
    public data class Failure(public val reason: String) : InstallResult()
}

public data class DesktopOsCapability(
    public val osFamily: OsFamily,
    public val hasSchtasks: Boolean,
    public val hasLaunchctl: Boolean,
    public val hasSystemctlUser: Boolean,
    public val hasCron: Boolean,
    public val notes: String,
)

public enum class OsFamily { WINDOWS, MACOS, LINUX, OTHER }

/**
 * Factory that returns the appropriate installer for the current OS.
 *
 * alpha05 returns a [StubInstaller] that logs the requested operation but doesn't
 * actually invoke schtasks/launchctl/systemctl. Real per-OS implementations land
 * per OS in v3.0.0-alpha05.X follow-ups.
 */
public fun createDesktopBackgroundInstaller(): DesktopBackgroundInstaller = StubInstaller()

internal class StubInstaller : DesktopBackgroundInstaller {
    override fun install(config: DesktopBackgroundConfig): InstallResult {
        co.touchlab.kermit.Logger.withTag("worker-kmp-daemon").w {
            "StubInstaller.install — alpha05 scaffold; actual per-OS schtasks/launchctl/systemctl wiring lands per-OS in alpha05.X. appId=${config.appId}"
        }
        return InstallResult.Failure("alpha05 scaffold — per-OS installer not yet implemented")
    }

    override fun uninstall(appId: String): InstallResult {
        co.touchlab.kermit.Logger.withTag("worker-kmp-daemon").w {
            "StubInstaller.uninstall — alpha05 scaffold. appId=$appId"
        }
        return InstallResult.Failure("alpha05 scaffold")
    }

    override fun isInstalled(appId: String): Boolean = false

    override fun probe(): DesktopOsCapability {
        val osName = System.getProperty("os.name").lowercase()
        val osFamily = when {
            osName.contains("win") -> OsFamily.WINDOWS
            osName.contains("mac") -> OsFamily.MACOS
            osName.contains("nux") || osName.contains("nix") -> OsFamily.LINUX
            else -> OsFamily.OTHER
        }
        return DesktopOsCapability(
            osFamily = osFamily,
            hasSchtasks = false,    // alpha05.X delivers ProcessBuilder-based probe
            hasLaunchctl = false,
            hasSystemctlUser = false,
            hasCron = false,
            notes = "alpha05 scaffold — full OS-capability probe lands in alpha05.X",
        )
    }
}
