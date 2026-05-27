package io.github.mobilebytelabs.worker.daemon

import io.github.mobilebytelabs.worker.config.DesktopBackgroundConfig

/**
 * Per-OS installer for the desktop daemon. Per-OS implementations:
 * - Windows: schtasks-based
 * - macOS: launchd user agent (~/Library/LaunchAgents/{appId}.worker-kmp.plist)
 * - Linux: systemd --user (~/.config/systemd/user/{appId}.worker-kmp.{timer,service}); cron fallback
 *
 * v3.0.0-alpha05.X (Phase 8 alpha05.X) replaces the alpha05 stub with real per-OS implementations
 * dispatched from
 * [io.github.mobilebytelabs.worker.daemon.installer.createDesktopBackgroundInstaller]:
 * - Windows → `WindowsTaskInstaller` (schtasks /Create /XML)
 * - macOS → `MacosLaunchdInstaller` (~/Library/LaunchAgents + launchctl load -w)
 * - Linux → `LinuxInstallerRouter` (systemctl --user preferred, crontab fallback)
 * - Other → `Failure("Unsupported OS")` returned from every operation
 *
 * `DesktopBackgroundConfig` itself moved to
 * `cmp-worker-kmp/.../config/DesktopBackgroundConfig.kt` (commonMain) so it can be a field
 * of `DesktopWorkerConfig.background` and consumers can declare the full config from
 * shared code.
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
 * Delegates to [io.github.mobilebytelabs.worker.daemon.installer.createDesktopBackgroundInstaller]
 * (under the `installer` package) so reflective lookups from `cmp-worker-desktop` can target a
 * stable fully-qualified name without coupling that module to this one at compile time.
 */
public fun createDesktopBackgroundInstaller(): DesktopBackgroundInstaller =
    io.github.mobilebytelabs.worker.daemon.installer.createDesktopBackgroundInstaller()

/**
 * Resolves [DesktopBackgroundConfig.persistenceDir] to a concrete absolute path, falling back to
 * `${user.home}/.worker-kmp` on the JVM when the consumer left it null in commonMain config.
 *
 * Added in v3.0.0-alpha05.X (Phase 8 alpha05.X).
 */
internal fun DesktopBackgroundConfig.resolvedPersistenceDir(): String =
    persistenceDir ?: "${System.getProperty("user.home")}/.worker-kmp"
